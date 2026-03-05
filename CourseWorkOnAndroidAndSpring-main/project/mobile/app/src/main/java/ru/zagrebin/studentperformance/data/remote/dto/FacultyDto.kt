package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

data class FacultyDto(
    @SerializedName("facultyId") val facultyId: Long,
    @SerializedName("name") val name: String,
    @SerializedName("deanName") val deanName: String? = null
)
