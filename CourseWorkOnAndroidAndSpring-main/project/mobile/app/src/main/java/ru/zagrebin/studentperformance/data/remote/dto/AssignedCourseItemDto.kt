package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AssignedCourseItemDto(
    @SerializedName("assignmentId") val assignmentId: Long,
    @SerializedName("subjectId") val subjectId: Long? = null,
    @SerializedName("subjectName") val subjectName: String? = null,
    @SerializedName("groupId") val groupId: Long? = null,
    @SerializedName("groupName") val groupName: String? = null,
    @SerializedName("academicYear") val academicYear: Int? = null,
    @SerializedName("semester") val semester: Int? = null
)
