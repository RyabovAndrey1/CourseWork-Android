package ru.ryabov.studentperformance.data.local.dao

import androidx.room.*
import ru.ryabov.studentperformance.data.local.entity.GradeTypeEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с типами оценок
 */
@Dao
interface GradeTypeDao {

    @Query("SELECT * FROM grade_types ORDER BY name")
    fun getAllGradeTypes(): Flow<List<GradeTypeEntity>>

    @Query("SELECT * FROM grade_types WHERE typeId = :typeId")
    suspend fun getGradeTypeById(typeId: Long): GradeTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeType(gradeType: GradeTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGradeTypes(gradeTypes: List<GradeTypeEntity>)

    @Update
    suspend fun updateGradeType(gradeType: GradeTypeEntity)

    @Delete
    suspend fun deleteGradeType(gradeType: GradeTypeEntity)

    @Query("DELETE FROM grade_types")
    suspend fun deleteAllGradeTypes()
}