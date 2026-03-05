package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Обёртка ответа REST API (success, message, data).
 */
data class ApiResponse<T>(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null
)
