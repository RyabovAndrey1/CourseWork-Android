package ru.ryabov.studentperformance.dto.grade;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GradeDto {

    private Long gradeId;
    private Long studentId;
    private String studentFullName;
    private Long subjectId;
    private String subjectName;
    private String gradeTypeName;
    private BigDecimal gradeValue;
    private LocalDate gradeDate;
    private Integer semester;
    private Integer academicYear;
    private String comment;
    private String workType;

    public GradeDto() {
    }

    public GradeDto(Long gradeId, Long studentId, String studentFullName, Long subjectId,
                    String subjectName, String gradeTypeName, BigDecimal gradeValue,
                    LocalDate gradeDate, Integer semester, Integer academicYear,
                    String comment, String workType) {
        this.gradeId = gradeId;
        this.studentId = studentId;
        this.studentFullName = studentFullName;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.gradeTypeName = gradeTypeName;
        this.gradeValue = gradeValue;
        this.gradeDate = gradeDate;
        this.semester = semester;
        this.academicYear = academicYear;
        this.comment = comment;
        this.workType = workType;
    }

    // Getters and Setters
    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentFullName() { return studentFullName; }
    public void setStudentFullName(String studentFullName) { this.studentFullName = studentFullName; }

    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getGradeTypeName() { return gradeTypeName; }
    public void setGradeTypeName(String gradeTypeName) { this.gradeTypeName = gradeTypeName; }

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
