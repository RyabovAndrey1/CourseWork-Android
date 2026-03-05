package ru.ryabov.studentperformance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "curriculum_subjects")
public class CurriculumSubject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "curriculum_id", nullable = false)
    private Integer curriculumId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "semester", nullable = false)
    private Integer semester;

    @Column(name = "hours_lecture")
    private Integer hoursLecture;

    @Column(name = "hours_practice")
    private Integer hoursPractice;

    @Column(name = "hours_lab")
    private Integer hoursLab;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public CurriculumSubject() {
    }

    public CurriculumSubject(Integer curriculumId, Subject subject, Integer semester,
                             Integer hoursLecture, Integer hoursPractice, Integer hoursLab) {
        this.curriculumId = curriculumId;
        this.subject = subject;
        this.semester = semester;
        this.hoursLecture = hoursLecture;
        this.hoursPractice = hoursPractice;
        this.hoursLab = hoursLab;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getCurriculumId() { return curriculumId; }
    public void setCurriculumId(Integer curriculumId) { this.curriculumId = curriculumId; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public Integer getHoursLecture() { return hoursLecture; }
    public void setHoursLecture(Integer hoursLecture) { this.hoursLecture = hoursLecture; }

    public Integer getHoursPractice() { return hoursPractice; }
    public void setHoursPractice(Integer hoursPractice) { this.hoursPractice = hoursPractice; }

    public Integer getHoursLab() { return hoursLab; }
    public void setHoursLab(Integer hoursLab) { this.hoursLab = hoursLab; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getSubjectName() {
        if (subject != null) {
            return subject.getName();
        }
        return "";
    }
}