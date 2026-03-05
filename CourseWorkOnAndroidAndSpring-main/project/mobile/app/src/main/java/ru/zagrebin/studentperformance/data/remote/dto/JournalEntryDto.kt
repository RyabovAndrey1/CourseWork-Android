package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName

/** Запись журнала: оценка или посещаемость (entryType: "Оценка" | "Посещаемость"). */
data class JournalEntryDto(
    @SerializedName("date") val date: String? = null,
    @SerializedName("studentFullName") val studentFullName: String? = null,
    @SerializedName("groupName") val groupName: String? = null,
    @SerializedName("subjectName") val subjectName: String? = null,
    @SerializedName("entryType") val entryType: String? = null,
    @SerializedName("typeDetail") val typeDetail: String? = null,
    @SerializedName("valueDisplay") val valueDisplay: String? = null
)
