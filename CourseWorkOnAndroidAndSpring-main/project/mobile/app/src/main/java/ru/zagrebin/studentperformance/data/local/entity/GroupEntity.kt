package ru.ryabov.studentperformance.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Сущность учебной группы в локальной БД
 */
@Entity(
    tableName = "study_groups",
    foreignKeys = [
        ForeignKey(
            entity = FacultyEntity::class,
            parentColumns = ["facultyId"],
            childColumns = ["facultyId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("facultyId")]
)
@Parcelize
data class GroupEntity(
    @PrimaryKey
    val groupId: Long,
    val name: String,
    val facultyId: Long?,
    val admissionYear: Int?,
    val specialization: String?,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable