package ru.ryabov.studentperformance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "credits", nullable = false, precision = 4, scale = 1)
    private BigDecimal credits;

    @Column(name = "total_hours")
    private Integer totalHours;

    @Column(name = "lecture_hours")
    private Integer lectureHours;

    @Column(name = "practice_hours")
    private Integer practiceHours;

    @Column(name = "lab_hours")
    private Integer labHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "control_type", length = 20)
    private ControlType controlType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "subject")
    private List<Grade> grades = new ArrayList<>();

    @OneToMany(mappedBy = "subject")
    private List<FinalGrade> finalGrades = new ArrayList<>();

    @OneToMany(mappedBy = "subject")
    private List<AssignedCourse> assignedCourses = new ArrayList<>();

    public enum ControlType {
        EXAM,      // Экзамен
        CREDIT,    // Зачет
        DIFF_CREDIT // Дифференцированный зачет
    }

    public Subject() {
    }

    public Subject(String name, String code, BigDecimal credits, Integer totalHours,
                   Integer lectureHours, Integer practiceHours, Integer labHours,
                   ControlType controlType, String description) {
        this.name = name;
        this.code = code;
        this.credits = credits;
        this.totalHours = totalHours;
        this.lectureHours = lectureHours;
        this.practiceHours = practiceHours;
        this.labHours = labHours;
        this.controlType = controlType;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }

    public Integer getTotalHours() { return totalHours; }
    public void setTotalHours(Integer totalHours) { this.totalHours = totalHours; }

    public Integer getLectureHours() { return lectureHours; }
    public void setLectureHours(Integer lectureHours) { this.lectureHours = lectureHours; }

    public Integer getPracticeHours() { return practiceHours; }
    public void setPracticeHours(Integer practiceHours) { this.practiceHours = practiceHours; }

    public Integer getLabHours() { return labHours; }
    public void setLabHours(Integer labHours) { this.labHours = labHours; }

    public ControlType getControlType() { return controlType; }
    public void setControlType(ControlType controlType) { this.controlType = controlType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Grade> getGrades() { return grades; }
    public void setGrades(List<Grade> grades) { this.grades = grades; }

    public List<FinalGrade> getFinalGrades() { return finalGrades; }
    public void setFinalGrades(List<FinalGrade> finalGrades) { this.finalGrades = finalGrades; }

    public List<AssignedCourse> getAssignedCourses() { return assignedCourses; }
    public void setAssignedCourses(List<AssignedCourse> assignedCourses) { this.assignedCourses = assignedCourses; }
}
