package ru.ryabov.studentperformance.data.local.dao

import androidx.room.*
import ru.ryabov.studentperformance.data.local.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с дисциплинами
 */
@Dao
interface SubjectDao {

    @Query("SELECT * FROM subjects ORDER BY name")
    fun getAllSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE subjectId = :subjectId")
    suspend fun getSubjectById(subjectId: Long): SubjectEntity?

    @Query("SELECT * FROM subjects WHERE subjectId = :subjectId")
    fun getSubjectByIdFlow(subjectId: Long): Flow<SubjectEntity?>

    @Query("SELECT * FROM subjects WHERE controlType = :controlType ORDER BY name")
    fun getSubjectsByControlType(controlType: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE name LIKE '%' || :query || '%' ORDER BY name")
    fun searchSubjects(query: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE code = :code")
    suspend fun getSubjectByCode(code: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)

    @Query("DELETE FROM subjects")
    suspend fun deleteAllSubjects()

    @Query("SELECT COUNT(*) FROM subjects")
    suspend fun getSubjectCount(): Int
}