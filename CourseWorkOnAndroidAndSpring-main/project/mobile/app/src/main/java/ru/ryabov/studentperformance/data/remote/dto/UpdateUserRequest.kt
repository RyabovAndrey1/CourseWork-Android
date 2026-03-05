package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Тело запроса на обновление пользователя. Все поля опциональны. */
data class UpdateUserRequest(
    @SerializedName("email") val email: String? = null,
    @SerializedName("lastName") val lastName: String? = null,
    @SerializedName("firstName") val firstName: String? = null,
    @SerializedName("middleName") val middleName: String? = null,
    @SerializedName("role") val role: String? = null,
    @SerializedName("password") val password: String? = null
)
