package ru.ryabov.studentperformance.data.local.dao

import androidx.room.*
import ru.ryabov.studentperformance.data.local.entity.FacultyEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с факультетами
 */
@Dao
interface FacultyDao {

    @Query("SELECT * FROM faculties ORDER BY name")
    fun getAllFaculties(): Flow<List<FacultyEntity>>

    @Query("SELECT * FROM faculties WHERE facultyId = :facultyId")
    suspend fun getFacultyById(facultyId: Long): FacultyEntity?

    @Query("SELECT * FROM faculties WHERE facultyId = :facultyId")
    fun getFacultyByIdFlow(facultyId: Long): Flow<FacultyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaculty(faculty: FacultyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFaculties(faculties: List<FacultyEntity>)

    @Update
    suspend fun updateFaculty(faculty: FacultyEntity)

    @Delete
    suspend fun deleteFaculty(faculty: FacultyEntity)

    @Query("DELETE FROM faculties")
    suspend fun deleteAllFaculties()

    @Query("SELECT COUNT(*) FROM faculties")
    suspend fun getFacultyCount(): Int
}