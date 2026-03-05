package ru.ryabov.studentperformance.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Сущность преподавателя в локальной БД
 */
@Entity(
    tableName = "teachers",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
@Parcelize
data class TeacherEntity(
    @PrimaryKey
    val teacherId: Long,
    val userId: Long,
    val departmentId: Long?,
    val academicDegree: String?,
    val position: String?,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable