package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class CreateUserRequest(
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("lastName") val lastName: String,
    @SerializedName("firstName") val firstName: String,
    @SerializedName("middleName") val middleName: String? = null,
    @SerializedName("role") val role: String
)
