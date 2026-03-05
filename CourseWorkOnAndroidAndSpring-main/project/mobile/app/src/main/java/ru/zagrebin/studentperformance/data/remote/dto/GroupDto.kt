package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class GroupDto(
    @SerializedName("groupId") val groupId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("facultyName") val facultyName: String? = null,
    @SerializedName("facultyId") val facultyId: Long? = null,
    @SerializedName("admissionYear") val admissionYear: Int? = null,
    @SerializedName("specialization") val specialization: String? = null,
    @SerializedName("studentCount") val studentCount: Int = 0
)
