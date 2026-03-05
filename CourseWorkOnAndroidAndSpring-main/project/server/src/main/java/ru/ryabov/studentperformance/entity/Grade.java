package ru.ryabov.studentperformance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "grades")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "grade_id")
    private Long gradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private AssignedCourse assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_type_id")
    private GradeType gradeType;

    @Column(name = "grade_value", precision = 3, scale = 2)
    private BigDecimal gradeValue;

    @Column(name = "grade_date", nullable = false)
    private LocalDate gradeDate;

    @Column(name = "semester")
    private Integer semester;

    @Column(name = "academic_year")
    private Integer academicYear;

    @Column(name = "comment", length = 500)
    private String comment;

    @Column(name = "work_type", length = 100)
    private String workType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Grade() {
    }

    public Grade(Student student, Subject subject, AssignedCourse assignment,
                 GradeType gradeType, BigDecimal gradeValue, LocalDate gradeDate,
                 Integer semester, Integer academicYear, String comment, String workType) {
        this.student = student;
        this.subject = subject;
        this.assignment = assignment;
        this.gradeType = gradeType;
        this.gradeValue = gradeValue;
        this.gradeDate = gradeDate;
        this.semester = semester;
        this.academicYear = academicYear;
        this.comment = comment;
        this.workType = workType;
        this.createdAt = LocalDateTime.now();
        this.gradeDate = LocalDate.now();
    }

    // Getters and Setters
    public Long getGradeId() { return gradeId; }
    public void setGradeId(Long gradeId) { this.gradeId = gradeId; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public AssignedCourse getAssignment() { return assignment; }
    public void setAssignment(AssignedCourse assignment) { this.assignment = assignment; }

    public GradeType getGradeType() { return gradeType; }
    public void setGradeType(GradeType gradeType) { this.gradeType = gradeType; }

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

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getGradeTypeName() {
        if (gradeType != null) {
            return gradeType.getName();
        }
        return "";
    }
}