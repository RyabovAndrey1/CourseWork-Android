package ru.ryabov.studentperformance.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class GradeDto(
    @SerializedName("gradeId") val gradeId: Long,
    @SerializedName("studentId") val studentId: Long,
    @SerializedName("studentFullName") val studentFullName: String? = null,
    @SerializedName("subjectId") val subjectId: Long,
    @SerializedName("subjectName") val subjectName: String? = null,
    @SerializedName("gradeTypeName") val gradeTypeName: String? = null,
    @SerializedName("gradeValue") val gradeValue: BigDecimal? = null,
    @SerializedName("gradeDate") val gradeDate: String? = null,
    @SerializedName("semester") val semester: Int? = null,
    @SerializedName("academicYear") val academicYear: Int? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("workType") val workType: String? = null
)

data class CreateGradeRequest(
    @SerializedName("studentId") val studentId: Long,
    @SerializedName("subjectId") val subjectId: Long,
    @SerializedName("assignmentId") val assignmentId: Long? = null,
    @SerializedName("gradeTypeId") val gradeTypeId: Long? = null,
    @SerializedName("gradeValue") val gradeValue: BigDecimal? = null,
    @SerializedName("gradeDate") val gradeDate: String? = null,
    @SerializedName("semester") val semester: Int? = null,
    @SerializedName("academicYear") val academicYear: Int? = null,
    @SerializedName("comment") val comment: String? = null,
    @SerializedName("workType") val workType: String? = null
)

data class GradeSummaryDto(
    @SerializedName("averageGrade") val averageGrade: java.math.BigDecimal? = null,
    @SerializedName("totalGrades") val totalGrades: Int = 0,
    @SerializedName("excellentCount") val excellentCount: Int = 0,
    @SerializedName("goodCount") val goodCount: Int = 0,
    @SerializedName("satisfactoryCount") val satisfactoryCount: Int = 0,
    @SerializedName("failCount") val failCount: Int = 0
)
