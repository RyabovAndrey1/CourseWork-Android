package ru.ryabov.studentperformance.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Сущность факультета в локальной БД
 */
@Entity(tableName = "faculties")
@Parcelize
data class FacultyEntity(
    @PrimaryKey
    val facultyId: Long,
    val name: String,
    val deanName: String?,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable