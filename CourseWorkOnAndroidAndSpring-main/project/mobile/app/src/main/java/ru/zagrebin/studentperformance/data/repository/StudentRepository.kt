package ru.ryabov.studentperformance.data.repository

import kotlinx.coroutines.flow.Flow
import ru.ryabov.studentperformance.data.local.dao.*
import ru.ryabov.studentperformance.data.local.entity.*

/**
 * Репозиторий для доступа к данным студенческой успеваемости
 * Реализует паттерн Repository для абстракции источников данных
 */
class StudentRepository(
    private val userDao: UserDao,
    private val studentDao: StudentDao,
    private val gradeDao: GradeDao,
    private val subjectDao: SubjectDao,
    private val groupDao: GroupDao,
    private val facultyDao: FacultyDao,
    private val gradeTypeDao: GradeTypeDao
) {
    // ===== Users =====
    fun getAllUsers(): Flow<List<UserEntity>> = userDao.getAllUsers()
    fun getUsersByRole(role: String): Flow<List<UserEntity>> = userDao.getUsersByRole(role)
    suspend fun getUserById(userId: Long): UserEntity? = userDao.getUserById(userId)
    fun getUserByIdFlow(userId: Long): Flow<UserEntity?> = userDao.getUserByIdFlow(userId)
    suspend fun getUserByLogin(login: String): UserEntity? = userDao.getUserByLogin(login)
    suspend fun insertUser(user: UserEntity) = userDao.insertUser(user)
    suspend fun updateUser(user: UserEntity) = userDao.updateUser(user)
    suspend fun setUserActive(userId: Long, isActive: Boolean) = userDao.setUserActive(userId, isActive)
    suspend fun getUserCount(): Int = userDao.getUserCount()

    // ===== Students =====
    fun getAllStudents(): Flow<List<StudentEntity>> = studentDao.getAllStudents()
    suspend fun getStudentById(studentId: Long): StudentEntity? = studentDao.getStudentById(studentId)
    fun getStudentByIdFlow(studentId: Long): Flow<StudentEntity?> = studentDao.getStudentByIdFlow(studentId)
    fun getStudentsByGroup(groupId: Long): Flow<List<StudentEntity>> = studentDao.getStudentsByGroup(groupId)
    suspend fun getStudentByUserId(userId: Long): StudentEntity? = studentDao.getStudentByUserId(userId)
    suspend fun insertStudent(student: StudentEntity) = studentDao.insertStudent(student)
    suspend fun updateStudent(student: StudentEntity) = studentDao.updateStudent(student)
    suspend fun assignStudentToGroup(studentId: Long, groupId: Long) = studentDao.assignStudentToGroup(studentId, groupId)
    suspend fun getStudentCount(): Int = studentDao.getStudentCount()

    // ===== Grades =====
    fun getAllGrades(): Flow<List<GradeEntity>> = gradeDao.getAllGrades()
    suspend fun getGradeById(gradeId: Long): GradeEntity? = gradeDao.getGradeById(gradeId)
    fun getGradesByStudent(studentId: Long): Flow<List<GradeEntity>> = gradeDao.getGradesByStudent(studentId)
    suspend fun getGradesByStudentSync(studentId: Long): List<GradeEntity> = gradeDao.getGradesByStudentSync(studentId)
    fun getGradesByStudentAndSubject(studentId: Long, subjectId: Long): Flow<List<GradeEntity>> =
        gradeDao.getGradesByStudentAndSubject(studentId, subjectId)
    fun getGradesBySubject(subjectId: Long): Flow<List<GradeEntity>> = gradeDao.getGradesBySubject(subjectId)
    suspend fun getAverageGradeByStudent(studentId: Long): Double? = gradeDao.getAverageGradeByStudent(studentId)
    suspend fun getAverageGradeByStudentAndSubject(studentId: Long, subjectId: Long): Double? =
        gradeDao.getAverageGradeByStudentAndSubject(studentId, subjectId)
    suspend fun getGradeCountByStudent(studentId: Long): Int = gradeDao.getGradeCountByStudent(studentId)
    suspend fun insertGrade(grade: GradeEntity): Long = gradeDao.insertGrade(grade)
    suspend fun insertGrades(grades: List<GradeEntity>) = gradeDao.insertGrades(grades)
    suspend fun deleteGradesByStudent(studentId: Long) = gradeDao.deleteGradesByStudent(studentId)
    suspend fun updateGrade(grade: GradeEntity) = gradeDao.updateGrade(grade)
    suspend fun deleteGradeById(gradeId: Long) = gradeDao.deleteGradeById(gradeId)

    // ===== Subjects =====
    fun getAllSubjects(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjects()
    suspend fun getSubjectById(subjectId: Long): SubjectEntity? = subjectDao.getSubjectById(subjectId)
    fun getSubjectByIdFlow(subjectId: Long): Flow<SubjectEntity?> = subjectDao.getSubjectByIdFlow(subjectId)
    fun getSubjectsByControlType(controlType: String): Flow<List<SubjectEntity>> =
        subjectDao.getSubjectsByControlType(controlType)
    fun searchSubjects(query: String): Flow<List<SubjectEntity>> = subjectDao.searchSubjects(query)
    suspend fun insertSubject(subject: SubjectEntity): Long = subjectDao.insertSubject(subject)
    suspend fun updateSubject(subject: SubjectEntity) = subjectDao.updateSubject(subject)
    suspend fun getSubjectCount(): Int = subjectDao.getSubjectCount()

    // ===== Groups =====
    fun getAllGroups(): Flow<List<GroupEntity>> = groupDao.getAllGroups()
    suspend fun getGroupById(groupId: Long): GroupEntity? = groupDao.getGroupById(groupId)
    fun getGroupByIdFlow(groupId: Long): Flow<GroupEntity?> = groupDao.getGroupByIdFlow(groupId)
    fun getGroupsByFaculty(facultyId: Long): Flow<List<GroupEntity>> = groupDao.getGroupsByFaculty(facultyId)
    fun getGroupsByYear(year: Int): Flow<List<GroupEntity>> = groupDao.getGroupsByYear(year)
    suspend fun insertGroup(group: GroupEntity): Long = groupDao.insertGroup(group)
    suspend fun updateGroup(group: GroupEntity) = groupDao.updateGroup(group)
    suspend fun getGroupCount(): Int = groupDao.getGroupCount()
    suspend fun getStudentCountByGroup(groupId: Long): Int = studentDao.getStudentCountByGroup(groupId)

    // ===== Faculties =====
    fun getAllFaculties(): Flow<List<FacultyEntity>> = facultyDao.getAllFaculties()
    suspend fun getFacultyById(facultyId: Long): FacultyEntity? = facultyDao.getFacultyById(facultyId)
    fun getFacultyByIdFlow(facultyId: Long): Flow<FacultyEntity?> = facultyDao.getFacultyByIdFlow(facultyId)
    suspend fun insertFaculty(faculty: FacultyEntity): Long = facultyDao.insertFaculty(faculty)
    suspend fun updateFaculty(faculty: FacultyEntity) = facultyDao.updateFaculty(faculty)
    suspend fun getFacultyCount(): Int = facultyDao.getFacultyCount()

    // ===== Grade Types =====
    fun getAllGradeTypes(): Flow<List<GradeTypeEntity>> = gradeTypeDao.getAllGradeTypes()
    suspend fun getGradeTypeById(typeId: Long): GradeTypeEntity? = gradeTypeDao.getGradeTypeById(typeId)
    suspend fun insertGradeTypes(gradeTypes: List<GradeTypeEntity>) = gradeTypeDao.insertGradeTypes(gradeTypes)
}