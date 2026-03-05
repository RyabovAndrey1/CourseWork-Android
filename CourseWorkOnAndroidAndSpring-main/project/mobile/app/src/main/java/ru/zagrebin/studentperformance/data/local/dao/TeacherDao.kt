package ru.ryabov.studentperformance.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ru.ryabov.studentperformance.data.local.entity.TeacherEntity

/**
 * DAO для работы с преподавателями в локальной БД
 */
@Dao
interface TeacherDao {

    @Query("SELECT * FROM teachers ORDER BY teacherId")
    fun getAllTeachers(): Flow<List<TeacherEntity>>

    @Query("SELECT * FROM teachers WHERE teacherId = :teacherId")
    suspend fun getTeacherById(teacherId: Long): TeacherEntity?

    @Query("SELECT * FROM teachers WHERE userId = :userId")
    suspend fun getTeacherByUserId(userId: Long): TeacherEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeacher(teacher: TeacherEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<TeacherEntity>)

    @Update
    suspend fun updateTeacher(teacher: TeacherEntity)

    @Delete
    suspend fun deleteTeacher(teacher: TeacherEntity)

    @Query("DELETE FROM teachers")
    suspend fun deleteAllTeachers()
}
