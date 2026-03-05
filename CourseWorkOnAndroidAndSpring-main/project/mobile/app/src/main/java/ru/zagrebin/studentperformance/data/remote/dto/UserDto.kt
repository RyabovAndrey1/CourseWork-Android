package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("userId") val userId: Long,
    @SerializedName("login") val login: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("middleName") val middleName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("isActive") val isActive: Boolean = true
)
