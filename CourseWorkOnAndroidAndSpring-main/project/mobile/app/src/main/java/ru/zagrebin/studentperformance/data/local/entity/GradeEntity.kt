package ru.ryabov.studentperformance.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Сущность оценки в локальной БД
 */
@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["studentId"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["subjectId"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("studentId"), Index("subjectId")]
)
@Parcelize
data class GradeEntity(
    @PrimaryKey(autoGenerate = true)
    val gradeId: Long = 0,
    val studentId: Long,
    val subjectId: Long,
    val gradeTypeId: Long?,
    val gradeValue: Double?,
    val gradeDate: Long,
    val semester: Int?,
    val academicYear: Int?,
    val comment: String?,
    val workType: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long? = null
) : Parcelable