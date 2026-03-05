package ru.ryabov.studentperformance.data.sync

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.ryabov.studentperformance.StudentPerformanceApp
import ru.ryabov.studentperformance.data.local.StudentDatabase
import ru.ryabov.studentperformance.data.local.entity.FacultyEntity
import ru.ryabov.studentperformance.data.local.entity.GroupEntity
import ru.ryabov.studentperformance.data.local.entity.StudentEntity
import ru.ryabov.studentperformance.data.local.entity.SubjectEntity
import ru.ryabov.studentperformance.data.local.entity.UserEntity
import ru.ryabov.studentperformance.data.remote.RetrofitProvider
import ru.ryabov.studentperformance.data.remote.SessionManager
import ru.ryabov.studentperformance.domain.ISyncCatalog

/**
 * Синхронизация данных с сервером: загрузка факультетов, групп, дисциплин и студентов в локальную БД.
 * Реализует [ISyncCatalog] для использования из Domain-слоя (Use Case, WorkManager).
 */
class SyncManager(
    private val context: Context,
    private val database: StudentDatabase
) : ISyncCatalog {

    private val sessionManager = SessionManager(context.applicationContext)
    private val api by lazy { RetrofitProvider.createApi(sessionManager) }

    /**
     * Загружает с сервера справочники и студентов, сохраняет в Room.
     * Вызывать после входа или по кнопке «Обновить».
     */
    override suspend fun sync(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!sessionManager.isLoggedIn()) return@withContext Result.failure(IllegalStateException("Нет сессии"))
        runCatching {
            syncFaculties()
            syncGroups()
            syncSubjects()
            syncStudents()
        }
    }

    private suspend fun syncFaculties() {
        val response = api.getFaculties()
        if (!response.isSuccessful) return
        val list = response.body()?.data ?: return
        val entities = list.map { FacultyEntity(it.facultyId, it.name, it.deanName) }
        database.facultyDao().insertFaculties(entities)
    }

    private suspend fun syncGroups() {
        val response = api.getGroups()
        if (!response.isSuccessful) return
        val list = response.body()?.data ?: return
        val entities = list.map { GroupEntity(it.groupId, it.name, it.facultyId, it.admissionYear, it.specialization) }
        database.groupDao().insertGroups(entities)
    }

    private suspend fun syncSubjects() {
        val response = api.getSubjects()
        if (!response.isSuccessful) return
        val list = response.body()?.data ?: return
        val entities = list.map {
            SubjectEntity(
                subjectId = it.subjectId,
                name = it.name,
                code = it.code,
                credits = it.credits?.toDouble() ?: 0.0,
                totalHours = it.totalHours,
                lectureHours = it.lectureHours,
                practiceHours = it.practiceHours,
                labHours = it.labHours,
                controlType = it.controlType,
                description = it.description
            )
        }
        database.subjectDao().insertSubjects(entities)
    }

    private suspend fun syncStudents() {
        val response = api.getStudents()
        if (!response.isSuccessful) return
        val list = response.body()?.data ?: return
        val now = System.currentTimeMillis()
        for (dto in list) {
            val userId = dto.userId ?: continue
            val stubUser = UserEntity(
                userId = userId,
                login = dto.login ?: "",
                email = dto.email ?: "",
                lastName = dto.fullName ?: "",
                firstName = "",
                middleName = null,
                role = "STUDENT",
                isActive = true
            )
            database.userDao().insertUser(stubUser)
            val student = StudentEntity(
                studentId = dto.studentId,
                userId = userId,
                groupId = dto.groupId,
                recordBookNumber = dto.recordBookNumber,
                admissionYear = dto.admissionYear,
                birthDate = null,
                phoneNumber = null,
                address = null
            )
            database.studentDao().insertStudent(student)
        }
    }
}
