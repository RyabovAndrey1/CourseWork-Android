package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    @SerializedName("login") val login: String,
    @SerializedName("password") val password: String,
    @SerializedName("email") val email: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("middleName") val middleName: String? = null,
    @SerializedName("role") val role: String? = null
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("userId") val userId: Long,
    @SerializedName("login") val login: String,
    @SerializedName("email") val email: String?,
    @SerializedName("fullName") val fullName: String?,
    @SerializedName("role") val role: String
)

data class ForgotPasswordRequest(@SerializedName("email") val email: String)

data class ChangePasswordRequest(
    @SerializedName("oldPassword") val oldPassword: String,
    @SerializedName("newPassword") val newPassword: String
)

data class FcmTokenRequest(@SerializedName("token") val token: String)
