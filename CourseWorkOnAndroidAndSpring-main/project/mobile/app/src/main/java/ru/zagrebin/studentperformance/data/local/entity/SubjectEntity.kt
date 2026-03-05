package ru.ryabov.studentperformance.data.local.entity

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * Сущность дисциплины в локальной БД
 */
@Entity(tableName = "subjects")
@Parcelize
data class SubjectEntity(
    @PrimaryKey
    val subjectId: Long,
    val name: String,
    val code: String?,
    val credits: Double,
    val totalHours: Int?,
    val lectureHours: Int?,
    val practiceHours: Int?,
    val labHours: Int?,
    val controlType: String?,
    val description: String?,
    val createdAt: Long = System.currentTimeMillis()
) : Parcelable {

    val controlTypeEnum: ControlType?
        get() = controlType?.let { try { ControlType.valueOf(it) } catch (e: Exception) { null } }
}

enum class ControlType {
    EXAM, CREDIT, DIFF_CREDIT
}