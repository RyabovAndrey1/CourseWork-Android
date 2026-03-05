package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateStudentRequest(
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String? = null,
    @SerializedName("email") val email: String,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("middleName") val middleName: String? = null,
    @SerializedName("groupId") val groupId: Long? = null,
    @SerializedName("recordBookNumber") val recordBookNumber: String? = null,
    @SerializedName("admissionYear") val admissionYear: Int? = null
)
