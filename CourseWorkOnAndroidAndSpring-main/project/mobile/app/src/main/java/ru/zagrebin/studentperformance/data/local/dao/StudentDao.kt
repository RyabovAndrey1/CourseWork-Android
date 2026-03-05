package ru.ryabov.studentperformance.data.local.dao

import androidx.room.*
import ru.ryabov.studentperformance.data.local.entity.StudentEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы со студентами
 */
@Dao
interface StudentDao {

    @Query("SELECT * FROM students ORDER BY studentId")
    fun getAllStudents(): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE studentId = :studentId")
    suspend fun getStudentById(studentId: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE studentId = :studentId")
    fun getStudentByIdFlow(studentId: Long): Flow<StudentEntity?>

    @Query("SELECT * FROM students WHERE groupId = :groupId")
    fun getStudentsByGroup(groupId: Long): Flow<List<StudentEntity>>

    @Query("SELECT * FROM students WHERE userId = :userId")
    suspend fun getStudentByUserId(userId: Long): StudentEntity?

    @Query("SELECT * FROM students WHERE recordBookNumber = :recordBookNumber")
    suspend fun getStudentByRecordBook(recordBookNumber: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>)

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Delete
    suspend fun deleteStudent(student: StudentEntity)

    @Query("DELETE FROM students")
    suspend fun deleteAllStudents()

    @Query("UPDATE students SET groupId = :groupId WHERE studentId = :studentId")
    suspend fun assignStudentToGroup(studentId: Long, groupId: Long)

    @Query("SELECT COUNT(*) FROM students")
    suspend fun getStudentCount(): Int

    @Query("SELECT COUNT(*) FROM students WHERE groupId = :groupId")
    suspend fun getStudentCountByGroup(groupId: Long): Int
}