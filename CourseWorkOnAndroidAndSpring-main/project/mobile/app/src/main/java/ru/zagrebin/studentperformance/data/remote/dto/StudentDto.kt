package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class StudentDto(
    @SerializedName("studentId") val studentId: Long,
    @SerializedName("userId") val userId: Long? = null,
    @SerializedName("fullName") val fullName: String? = null,
    @SerializedName("login") val login: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("recordBookNumber") val recordBookNumber: String? = null,
    @SerializedName("groupName") val groupName: String? = null,
    @SerializedName("groupId") val groupId: Long? = null,
    @SerializedName("facultyName") val facultyName: String? = null,
    @SerializedName("admissionYear") val admissionYear: Int? = null
)
