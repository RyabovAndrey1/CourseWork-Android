package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TeacherDto(
    @SerializedName("teacherId") val teacherId: Long,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("login") val login: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("academicDegree") val academicDegree: String? = null,
    @SerializedName("position") val position: String? = null,
    @SerializedName("departmentName") val departmentName: String? = null,
    @SerializedName("facultyName") val facultyName: String? = null,
    @SerializedName("departmentId") val departmentId: Long? = null
)
