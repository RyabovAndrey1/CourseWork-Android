package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class DepartmentDto(
    @SerializedName("departmentId") val departmentId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("facultyId") val facultyId: Long? = null,
    @SerializedName("facultyName") val facultyName: String? = null,
    @SerializedName("headName") val headName: String? = null
)
