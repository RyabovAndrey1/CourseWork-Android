package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Явный тип ответа для GET /grade-types, чтобы Gson корректно десериализовал data как List<GradeTypeDto>.
 */
data class GradeTypeListResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: List<GradeTypeDto>? = null
)
