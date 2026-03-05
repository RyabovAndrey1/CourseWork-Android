package ru.ryabov.studentperformance.data.local.dao

import androidx.room.*
import ru.ryabov.studentperformance.data.local.entity.GradeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с оценками
 */
@Dao
interface GradeDao {

    @Query("SELECT * FROM grades ORDER BY gradeDate DESC")
    fun getAllGrades(): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE gradeId = :gradeId")
    suspend fun getGradeById(gradeId: Long): GradeEntity?

    @Query("SELECT * FROM grades WHERE studentId = :studentId ORDER BY gradeDate DESC")
    fun getGradesByStudent(studentId: Long): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE studentId = :studentId ORDER BY gradeDate DESC")
    suspend fun getGradesByStudentSync(studentId: Long): List<GradeEntity>

    @Query("SELECT * FROM grades WHERE studentId = :studentId AND subjectId = :subjectId ORDER BY gradeDate DESC")
    fun getGradesByStudentAndSubject(studentId: Long, subjectId: Long): Flow<List<GradeEntity>>

    @Query("SELECT * FROM grades WHERE subjectId = :subjectId ORDER BY gradeDate DESC")
    fun getGradesBySubject(subjectId: Long): Flow<List<GradeEntity>>

    @Query("SELECT AVG(gradeValue) FROM grades WHERE studentId = :studentId")
    suspend fun getAverageGradeByStudent(studentId: Long): Double?

    @Query("SELECT AVG(gradeValue) FROM grades WHERE studentId = :studentId AND subjectId = :subjectId")
    suspend fun getAverageGradeByStudentAndSubject(studentId: Long, subjectId: Long): Double?

    @Query("SELECT COUNT(*) FROM grades WHERE studentId = :studentId")
    suspend fun getGradeCountByStudent(studentId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrades(grades: List<GradeEntity>)

    @Update
    suspend fun updateGrade(grade: GradeEntity)

    @Delete
    suspend fun deleteGrade(grade: GradeEntity)

    @Query("DELETE FROM grades WHERE gradeId = :gradeId")
    suspend fun deleteGradeById(gradeId: Long)

    @Query("DELETE FROM grades WHERE studentId = :studentId")
    suspend fun deleteGradesByStudent(studentId: Long)

    @Query("DELETE FROM grades")
    suspend fun deleteAllGrades()
}