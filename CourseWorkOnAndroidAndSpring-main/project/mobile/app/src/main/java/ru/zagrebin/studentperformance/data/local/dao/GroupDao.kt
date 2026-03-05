package ru.ryabov.studentperformance.data.local.dao

import androidx.room.*
import ru.ryabov.studentperformance.data.local.entity.GroupEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO для работы с учебными группами
 */
@Dao
interface GroupDao {

    @Query("SELECT * FROM study_groups ORDER BY name")
    fun getAllGroups(): Flow<List<GroupEntity>>

    @Query("SELECT * FROM study_groups WHERE groupId = :groupId")
    suspend fun getGroupById(groupId: Long): GroupEntity?

    @Query("SELECT * FROM study_groups WHERE groupId = :groupId")
    fun getGroupByIdFlow(groupId: Long): Flow<GroupEntity?>

    @Query("SELECT * FROM study_groups WHERE facultyId = :facultyId ORDER BY name")
    fun getGroupsByFaculty(facultyId: Long): Flow<List<GroupEntity>>

    @Query("SELECT * FROM study_groups WHERE admissionYear = :year ORDER BY name")
    fun getGroupsByYear(year: Int): Flow<List<GroupEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: GroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<GroupEntity>)

    @Update
    suspend fun updateGroup(group: GroupEntity)

    @Delete
    suspend fun deleteGroup(group: GroupEntity)

    @Query("DELETE FROM study_groups")
    suspend fun deleteAllGroups()

    @Query("SELECT COUNT(*) FROM study_groups")
    suspend fun getGroupCount(): Int
}