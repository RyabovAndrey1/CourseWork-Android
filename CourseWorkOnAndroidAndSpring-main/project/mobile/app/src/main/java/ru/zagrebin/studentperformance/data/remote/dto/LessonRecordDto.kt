package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Одно занятие в журнале: дата, группа, предмет, тип, список студентов. */
data class LessonRecordDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("groupName") val groupName: String? = null,
    @SerializedName("subjectName") val subjectName: String? = null,
    @SerializedName("gradeTypeName") val gradeTypeName: String? = null,
    @SerializedName("students") val students: List<StudentLessonRowDto>? = null,
    @SerializedName("groupId") val groupId: Long? = null,
    @SerializedName("subjectId") val subjectId: Long? = null,
    @SerializedName("gradeTypeId") val gradeTypeId: Long? = null
)

/** Строка по студенту в занятии: имя, присутствие, баллы. */
data class StudentLessonRowDto(
    @SerializedName("studentName") val studentName: String? = null,
    @SerializedName("present") val present: Boolean = false,
    @SerializedName("points") val points: java.math.BigDecimal? = null
)

/** Ответ API journal/lesson-records: страница занятий. */
data class LessonRecordsPageDto(
    @SerializedName("content") val content: List<LessonRecordDto>? = null,
    @SerializedName("totalElements") val totalElements: Long = 0,
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("number") val number: Int = 0,
    @SerializedName("size") val size: Int = 0
)
