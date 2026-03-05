package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class GradeTypeDto(
    @SerializedName("typeId") val typeId: Long,
    @SerializedName("name") val name: String? = null,
    @SerializedName("weight") val weight: BigDecimal? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("code") val code: String? = null,
    @SerializedName("maxScore") val maxScore: Int? = null
)
