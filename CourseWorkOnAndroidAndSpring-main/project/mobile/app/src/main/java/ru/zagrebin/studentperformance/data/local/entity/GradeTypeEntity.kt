package ru.ryabov.studentperformance.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Сущность типа оценки в локальной БД
 */
@Entity(tableName = "grade_types")
@Parcelize
data class GradeTypeEntity(
    @PrimaryKey
    val typeId: Long,
    val name: String,
    val weight: Double?,
    val description: String?,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable