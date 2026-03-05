package ru.ryabov.studentperformance.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.data.local.entity.FacultyEntity
import ru.ryabov.studentperformance.data.local.entity.GroupEntity
import ru.ryabov.studentperformance.data.local.entity.StudentEntity
import ru.ryabov.studentperformance.data.local.entity.SubjectEntity
import ru.ryabov.studentperformance.data.repository.StudentRepository

/**
 * ViewModel для админ-панели
 */
class AdminViewModel(
    private val repository: StudentRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<AdminUiState>(AdminUiState.Initial)
    val uiState: LiveData<AdminUiState> = _uiState

    private val _statistics = MutableLiveData<SystemStatistics>()
    val statistics: LiveData<SystemStatistics> = _statistics

    private val _faculties = MutableLiveData<List<FacultyEntity>>(emptyList())
    val faculties: LiveData<List<FacultyEntity>> = _faculties

    private val _groups = MutableLiveData<List<GroupEntity>>(emptyList())
    val groups: LiveData<List<GroupEntity>> = _groups

    private val _subjects = MutableLiveData<List<SubjectEntity>>(emptyList())
    val subjects: LiveData<List<SubjectEntity>> = _subjects

    private val _students = MutableLiveData<List<StudentEntity>>(emptyList())
    val students: LiveData<List<StudentEntity>> = _students

    init {
        loadStatistics()
        loadAllData()
    }

    /**
     * Загрузка статистики системы
     */
    fun loadStatistics() {
        viewModelScope.launch {
            _uiState.value = AdminUiState.Loading
            try {
                val stats = SystemStatistics(
                    totalUsers = repository.getUserCount(),
                    totalStudents = repository.getStudentCount(),
                    totalGroups = repository.getGroupCount(),
                    totalFaculties = repository.getFacultyCount(),
                    totalSubjects = repository.getSubjectCount()
                )
                _statistics.value = stats
                _uiState.value = AdminUiState.Success
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error(e.message ?: "Ошибка загрузки статистики")
            }
        }
    }

    /**
     * Загрузка всех данных
     */
    private fun loadAllData() {
        viewModelScope.launch {
            try {
                repository.getAllFaculties().collectLatest { _faculties.value = it }
            } catch (e: Exception) { /* ignore */ }
        }
        viewModelScope.launch {
            try {
                repository.getAllGroups().collectLatest { _groups.value = it }
            } catch (e: Exception) { /* ignore */ }
        }
        viewModelScope.launch {
            try {
                repository.getAllSubjects().collectLatest { _subjects.value = it }
            } catch (e: Exception) { /* ignore */ }
        }
        viewModelScope.launch {
            try {
                repository.getAllStudents().collectLatest { _students.value = it }
            } catch (e: Exception) { /* ignore */ }
        }
    }

    /**
     * Добавление факультета
     */
    fun addFaculty(name: String, deanName: String?) {
        viewModelScope.launch {
            try {
                val faculty = FacultyEntity(
                    facultyId = 0,
                    name = name,
                    deanName = deanName
                )
                repository.insertFaculty(faculty)
                _uiState.value = AdminUiState.Success
                loadStatistics()
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error(e.message ?: "Ошибка добавления")
            }
        }
    }

    /**
     * Добавление группы
     */
    fun addGroup(name: String, facultyId: Long?, admissionYear: Int?, specialization: String?) {
        viewModelScope.launch {
            try {
                val group = GroupEntity(
                    groupId = 0,
                    name = name,
                    facultyId = facultyId,
                    admissionYear = admissionYear,
                    specialization = specialization
                )
                repository.insertGroup(group)
                _uiState.value = AdminUiState.Success
                loadStatistics()
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error(e.message ?: "Ошибка добавления")
            }
        }
    }

    /**
     * Добавление дисциплины
     */
    fun addSubject(
        name: String,
        code: String?,
        credits: Double,
        totalHours: Int?,
        controlType: String?
    ) {
        viewModelScope.launch {
            try {
                val subject = SubjectEntity(
                    subjectId = 0,
                    name = name,
                    code = code,
                    credits = credits,
                    totalHours = totalHours,
                    lectureHours = null,
                    practiceHours = null,
                    labHours = null,
                    controlType = controlType,
                    description = null
                )
                repository.insertSubject(subject)
                _uiState.value = AdminUiState.Success
                loadStatistics()
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error(e.message ?: "Ошибка добавления")
            }
        }
    }

    /**
     * Обновление данных
     */
    fun refresh() {
        loadStatistics()
        loadAllData()
    }

    class Factory(private val repository: StudentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AdminViewModel::class.java)) {
                return AdminViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

/**
 * Состояние UI админ-панели
 */
sealed class AdminUiState {
    data object Initial : AdminUiState()
    data object Loading : AdminUiState()
    data object Success : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}

/**
 * Статистика системы
 */
data class SystemStatistics(
    val totalUsers: Int,
    val totalStudents: Int,
    val totalGroups: Int,
    val totalFaculties: Int,
    val totalSubjects: Int
)