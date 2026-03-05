package ru.ryabov.studentperformance.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.data.local.entity.GradeEntity
import ru.ryabov.studentperformance.data.local.entity.StudentEntity
import ru.ryabov.studentperformance.data.local.entity.SubjectEntity
import ru.ryabov.studentperformance.data.remote.StudentPerformanceApi
import ru.ryabov.studentperformance.data.remote.dto.CreateGradeRequest
import ru.ryabov.studentperformance.data.remote.dto.GradeDto
import ru.ryabov.studentperformance.data.repository.StudentRepository
import java.math.BigDecimal

/**
 * ViewModel для экрана успеваемости студента.
 * Если передан [api], добавление оценки отправляется на сервер и затем сохраняется локально.
 */
class GradeViewModel(
    private val repository: StudentRepository,
    private val api: StudentPerformanceApi? = null
) : ViewModel() {

    private val _uiState = MutableLiveData<GradeUiState>(GradeUiState.Loading)
    val uiState: LiveData<GradeUiState> = _uiState

    private val _grades = MutableLiveData<List<GradeWithDetails>>(emptyList())
    val grades: LiveData<List<GradeWithDetails>> = _grades

    private val _student = MutableLiveData<StudentEntity?>()
    val student: LiveData<StudentEntity?> = _student

    private val _averageGrade = MutableLiveData<Double?>()
    val averageGrade: LiveData<Double?> = _averageGrade

    private val _subjects = MutableLiveData<List<SubjectEntity>>()
    val subjects: LiveData<List<SubjectEntity>> = _subjects

    private val _totalGradesCount = MutableLiveData<Int>(0)
    val totalGradesCount: LiveData<Int> = _totalGradesCount

    private val _subjectSummaries = MutableLiveData<List<SubjectGradeSummary>>(emptyList())
    val subjectSummaries: LiveData<List<SubjectGradeSummary>> = _subjectSummaries

    /** Результат добавления оценки (для AddGradeActivity). true = успех, false = ошибка. */
    private val _addGradeResult = MutableLiveData<Boolean?>(null)
    val addGradeResult: LiveData<Boolean?> = _addGradeResult

    init {
        loadSubjects()
    }

    /**
     * Загрузка оценок для сводного экрана.
     * Для роли STUDENT — только свои оценки (сначала синхронизация с сервером); для TEACHER/ADMIN/DEANERY — все из Room.
     */
    fun loadAllGrades(currentUserId: Long? = null, userRole: String? = null) {
        viewModelScope.launch {
            _uiState.value = GradeUiState.Loading
            try {
                val studentIdFilter = if (userRole == "STUDENT" && currentUserId != null) {
                    repository.getStudentByUserId(currentUserId)?.studentId
                } else null

                if (studentIdFilter != null) {
                    if (api != null) syncGradesFromApi(studentIdFilter)
                    repository.getGradesByStudent(studentIdFilter).collectLatest { gradeList ->
                        val gradesWithDetails = gradeList.map { grade ->
                            val subject = repository.getSubjectById(grade.subjectId)
                            GradeWithDetails(grade, subject?.name ?: "—")
                        }
                        _grades.value = gradesWithDetails
                        _totalGradesCount.value = gradesWithDetails.size
                        _subjectSummaries.value = computeSubjectSummaries(gradesWithDetails)
                        val avg = repository.getAverageGradeByStudent(studentIdFilter)
                        _averageGrade.value = avg
                        _uiState.value = GradeUiState.Success
                    }
                } else {
                    repository.getAllGrades().collectLatest { gradeList ->
                        val gradesWithDetails = gradeList.map { grade ->
                            val subject = repository.getSubjectById(grade.subjectId)
                            GradeWithDetails(grade, subject?.name ?: "—")
                        }
                        _grades.value = gradesWithDetails
                        _totalGradesCount.value = gradesWithDetails.size
                        _subjectSummaries.value = computeSubjectSummaries(gradesWithDetails)
                        val avg = gradesWithDetails.mapNotNull { it.grade.gradeValue }.average().takeIf { !it.isNaN() }
                        _averageGrade.value = avg
                        _uiState.value = GradeUiState.Success
                    }
                }
            } catch (e: Exception) {
                _uiState.value = GradeUiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    /**
     * Загрузка оценок студента (с синхронизацией с сервером при наличии API).
     */
    fun loadStudentGrades(studentId: Long) {
        viewModelScope.launch {
            _uiState.value = GradeUiState.Loading

            try {
                if (api != null) syncGradesFromApi(studentId)
                // Загрузка данных студента
                launch {
                    repository.getStudentByIdFlow(studentId).collectLatest { student ->
                        _student.value = student
                    }
                }

                // Загрузка оценок
                launch {
                    repository.getGradesByStudent(studentId).collectLatest { gradeList ->
                        val gradesWithDetails = gradeList.map { grade ->
                            val subject = repository.getSubjectById(grade.subjectId)
                            GradeWithDetails(grade, subject?.name ?: "Неизвестно")
                        }
                        _grades.value = gradesWithDetails
                        _subjectSummaries.value = computeSubjectSummaries(gradesWithDetails)
                        _uiState.value = GradeUiState.Success
                    }
                }

                // Загрузка среднего балла
                launch {
                    val avg = repository.getAverageGradeByStudent(studentId)
                    _averageGrade.value = avg
                }

            } catch (e: Exception) {
                _uiState.value = GradeUiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    private fun computeSubjectSummaries(gradesWithDetails: List<GradeWithDetails>): List<SubjectGradeSummary> {
        return gradesWithDetails
            .groupBy { it.grade.subjectId }
            .map { (_, list) ->
                val first = list.first()
                val avg = list.mapNotNull { it.grade.gradeValue }.average().takeIf { !it.isNaN() } ?: 0.0
                SubjectGradeSummary(first.subjectName, first.grade.subjectId, avg, list.size)
            }
            .sortedBy { it.subjectName }
    }

    /**
     * Загрузка оценок по предмету
     */
    fun loadGradesBySubject(studentId: Long, subjectId: Long) {
        viewModelScope.launch {
            _uiState.value = GradeUiState.Loading
            try {
                repository.getGradesByStudentAndSubject(studentId, subjectId).collectLatest { grades ->
                    val gradesWithDetails = grades.map { grade ->
                        val subject = repository.getSubjectById(grade.subjectId)
                        GradeWithDetails(grade, subject?.name ?: "Неизвестно")
                    }
                    _grades.value = gradesWithDetails
                    _uiState.value = GradeUiState.Success
                }
            } catch (e: Exception) {
                _uiState.value = GradeUiState.Error(e.message ?: "Ошибка загрузки")
            }
        }
    }

    /**
     * Добавление новой оценки. При наличии API — сначала отправка на сервер, затем сохранение в Room.
     */
    fun addGrade(
        studentId: Long,
        subjectId: Long,
        gradeValue: Double,
        gradeTypeId: Long,
        comment: String?
    ) {
        viewModelScope.launch {
            try {
                if (api != null) {
                    val request = CreateGradeRequest(
                        studentId = studentId,
                        subjectId = subjectId,
                        gradeTypeId = gradeTypeId,
                        gradeValue = BigDecimal.valueOf(gradeValue),
                        semester = 1,
                        academicYear = 2024,
                        comment = comment,
                        workType = null
                    )
                    val response = api.createGrade(request)
                    if (response.isSuccessful) {
                        val dto = response.body()?.data
                        if (dto != null) {
                            val gradeDateMs = dto.gradeDate?.let { parseDateToMillis(it) } ?: System.currentTimeMillis()
                            val entity = GradeEntity(
                                gradeId = dto.gradeId,
                                studentId = dto.studentId,
                                subjectId = dto.subjectId,
                                gradeTypeId = gradeTypeId,
                                gradeValue = dto.gradeValue?.toDouble(),
                                gradeDate = gradeDateMs,
                                semester = dto.semester,
                                academicYear = dto.academicYear,
                                comment = dto.comment,
                                workType = dto.workType
                            )
                            repository.insertGrade(entity)
                            _averageGrade.value = repository.getAverageGradeByStudent(studentId)
                            _addGradeResult.value = true
                            return@launch
                        }
                    }
                }
                val currentTime = System.currentTimeMillis()
                val grade = GradeEntity(
                    studentId = studentId,
                    subjectId = subjectId,
                    gradeTypeId = gradeTypeId,
                    gradeValue = gradeValue,
                    gradeDate = currentTime,
                    semester = 1,
                    academicYear = 2024,
                    comment = comment,
                    workType = null
                )
                repository.insertGrade(grade)
                _averageGrade.value = repository.getAverageGradeByStudent(studentId)
                _addGradeResult.value = true
            } catch (e: Exception) {
                _uiState.value = GradeUiState.Error(e.message ?: "Ошибка добавления оценки")
                _addGradeResult.value = false
            }
        }
    }

    private fun parseDateToMillis(isoDate: String): Long {
        return try {
            java.time.LocalDate.parse(isoDate)
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    /** Синхронизация оценок студента с сервера в Room (вызов перед отображением списка). */
    private suspend fun syncGradesFromApi(studentId: Long) {
        val api = this.api ?: return
        val response = api.getGradesByStudent(studentId)
        if (!response.isSuccessful) return
        val list = response.body()?.data ?: return
        val entities = list.map { dto -> dtoToEntity(dto) }
        repository.deleteGradesByStudent(studentId)
        if (entities.isNotEmpty()) repository.insertGrades(entities)
    }

    private fun dtoToEntity(dto: GradeDto): ru.ryabov.studentperformance.data.local.entity.GradeEntity {
        val gradeDateMs = dto.gradeDate?.let { parseDateToMillis(it) } ?: System.currentTimeMillis()
        return ru.ryabov.studentperformance.data.local.entity.GradeEntity(
            gradeId = dto.gradeId,
            studentId = dto.studentId,
            subjectId = dto.subjectId,
            gradeTypeId = null,
            gradeValue = dto.gradeValue?.toDouble(),
            gradeDate = gradeDateMs,
            semester = dto.semester,
            academicYear = dto.academicYear,
            comment = dto.comment,
            workType = dto.workType
        )
    }

    /**
     * Удаление оценки
     */
    fun deleteGrade(gradeId: Long, studentId: Long) {
        viewModelScope.launch {
            try {
                repository.deleteGradeById(gradeId)
                _averageGrade.value = repository.getAverageGradeByStudent(studentId)
            } catch (e: Exception) {
                _uiState.value = GradeUiState.Error(e.message ?: "Ошибка удаления")
            }
        }
    }

    private fun loadSubjects() {
        viewModelScope.launch {
            repository.getAllSubjects().collectLatest {
                _subjects.value = it
            }
        }
    }

    /**
     * Получение статистики оценок
     */
    fun getGradeStatistics(studentId: Long) {
        viewModelScope.launch {
            val grades = repository.getGradesByStudentSync(studentId)
            var excellent = 0
            var good = 0
            var satisfactory = 0
            var fail = 0

            grades.forEach { grade ->
                grade.gradeValue?.let { value ->
                    when {
                        value >= 4.5 -> excellent++
                        value >= 3.5 -> good++
                        value >= 2.5 -> satisfactory++
                        else -> fail++
                    }
                }
            }

            _uiState.value = GradeUiState.Statistics(
                totalGrades = grades.size,
                excellent = excellent,
                good = good,
                satisfactory = satisfactory,
                fail = fail
            )
        }
    }

    class Factory(
        private val repository: StudentRepository,
        private val api: StudentPerformanceApi? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GradeViewModel::class.java)) {
                return GradeViewModel(repository, api) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Состояние UI экрана оценок
 */
sealed class GradeUiState {
    data object Loading : GradeUiState()
    data object Success : GradeUiState()
    data class Error(val message: String) : GradeUiState()
    data class Statistics(
        val totalGrades: Int,
        val excellent: Int,
        val good: Int,
        val satisfactory: Int,
        val fail: Int
    ) : GradeUiState()
}

/**
 * Оценка с дополнительной информацией о предмете
 */
data class GradeWithDetails(
    val grade: GradeEntity,
    val subjectName: String
)

/**
 * Сводка по дисциплине: название, средний балл, количество оценок
 */
data class SubjectGradeSummary(
    val subjectName: String,
    val subjectId: Long,
    val average: Double,
    val count: Int
)