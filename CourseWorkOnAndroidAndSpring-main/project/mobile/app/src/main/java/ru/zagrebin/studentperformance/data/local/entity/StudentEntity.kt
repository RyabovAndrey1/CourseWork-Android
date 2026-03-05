package ru.ryabov.studentperformance.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Сущность студента в локальной БД
 */
@Entity(
    tableName = "students",
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
data class StudentEntity(
    @PrimaryKey
    val studentId: Long,
    val userId: Long,
    val groupId: Long?,
    val recordBookNumber: String?,
    val admissionYear: Int?,
    val birthDate: Long?,
    val phoneNumber: String?,
    val address: String?,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable