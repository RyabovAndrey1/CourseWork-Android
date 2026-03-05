package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class SubjectDto(
    @SerializedName("subjectId") val subjectId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("code") val code: String? = null,
    @SerializedName("credits") val credits: BigDecimal? = null,
    @SerializedName("totalHours") val totalHours: Int? = null,
    @SerializedName("lectureHours") val lectureHours: Int? = null,
    @SerializedName("practiceHours") val practiceHours: Int? = null,
    @SerializedName("labHours") val labHours: Int? = null,
    @SerializedName("controlType") val controlType: String? = null,
    @SerializedName("description") val description: String? = null
)
