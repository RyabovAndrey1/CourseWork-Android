package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ReportRecordDto(
    @SerializedName("id") val id: Long,
    @SerializedName("reportType") val reportType: String?,
    @SerializedName("groupId") val groupId: Long?,
    @SerializedName("subjectId") val subjectId: Long?,
    @SerializedName("studentId") val studentId: Long?,
    @SerializedName("periodFrom") val periodFrom: String?,
    @SerializedName("periodTo") val periodTo: String?,
    @SerializedName("format") val format: String?,
    @SerializedName("createdAt") val createdAt: String?
)
