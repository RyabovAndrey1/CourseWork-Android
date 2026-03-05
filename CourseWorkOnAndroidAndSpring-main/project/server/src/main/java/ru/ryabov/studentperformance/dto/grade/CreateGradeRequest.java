package ru.ryabov.studentperformance.dto.grade;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class CreateGradeRequest {

    @NotNull(message = "Укажите студента")
    private Long studentId;

    @NotNull(message = "Укажите дисциплину")
    private Long subjectId;
    private Long assignmentId;
    private Long gradeTypeId;
    /** Баллы 0–40: лекция макс. 2, лаба/практика макс. 4, экзамен/зачёт макс. 40 */
    @DecimalMin(value = "0", message = "Балл не меньше 0")
    @DecimalMax(value = "40", message = "Балл не больше 40")
    private BigDecimal gradeValue;
    private LocalDate gradeDate;
    private Integer semester;
    private Integer academicYear;
    private String comment;
    private String workType;

    public CreateGradeRequest() {
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }

    public Long getGradeTypeId() { return gradeTypeId; }
    public void setGradeTypeId(Long gradeTypeId) { this.gradeTypeId = gradeTypeId; }

    public BigDecimal getGradeValue() { return gradeValue; }
    public void setGradeValue(BigDecimal gradeValue) { this.gradeValue = gradeValue; }

    public LocalDate getGradeDate() { return gradeDate; }
    public void setGradeDate(LocalDate gradeDate) { this.gradeDate = gradeDate; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public Integer getAcademicYear() { return academicYear; }
    public void setAcademicYear(Integer academicYear) { this.academicYear = academicYear; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getWorkType() { return workType; }
    public void setWorkType(String workType) { this.workType = workType; }
}