package ru.ryabov.studentperformance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "final_grades")
public class FinalGrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "final_grade_id")
    private Long finalGradeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "academic_year", nullable = false)
    private Integer academicYear;

    @Column(name = "final_grade", length = 10)
    private String finalGradeValue;

    @Column(name = "is_credited")
    private Boolean isCredited;

    @Column(name = "is_academic_debt")
    private Boolean isAcademicDebt = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public FinalGrade() {
    }

    public FinalGrade(Student student, Subject subject, Integer semester,
                      Integer academicYear, String finalGradeValue,
                      Boolean isCredited, Boolean isAcademicDebt) {
        this.student = student;
        this.subject = subject;
        this.semester = semester;
        this.academicYear = academicYear;
        this.finalGradeValue = finalGradeValue;
        this.isCredited = isCredited;
        this.isAcademicDebt = isAcademicDebt;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getFinalGradeId() { return finalGradeId; }
    public void setFinalGradeId(Long finalGradeId) { this.finalGradeId = finalGradeId; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public Integer getAcademicYear() { return academicYear; }
    public void setAcademicYear(Integer academicYear) { this.academicYear = academicYear; }

    public String getFinalGradeValue() { return finalGradeValue; }
    public void setFinalGradeValue(String finalGradeValue) { this.finalGradeValue = finalGradeValue; }

    public Boolean getIsCredited() { return isCredited; }
    public void setIsCredited(Boolean isCredited) { this.isCredited = isCredited; }

    public Boolean getIsAcademicDebt() { return isAcademicDebt; }
    public void setIsAcademicDebt(Boolean isAcademicDebt) { this.isAcademicDebt = isAcademicDebt; }

    public User getApprovedBy() { return approvedBy; }
    public void setApprovedBy(User approvedBy) { this.approvedBy = approvedBy; }

    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
