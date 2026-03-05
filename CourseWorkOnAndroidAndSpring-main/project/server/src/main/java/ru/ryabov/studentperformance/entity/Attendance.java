package ru.ryabov.studentperformance.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long attendanceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private AssignedCourse assignment;

    @Column(name = "lesson_date", nullable = false)
    private LocalDate lessonDate;

    @Column(name = "present", nullable = false)
    private Boolean present = true;

    @Column(name = "semester")
    private Integer semester;

    @Column(name = "academic_year")
    private Integer academicYear;

    @Column(name = "comment", length = 255)
    private String comment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public Attendance() {
    }

    public Attendance(Student student, Subject subject, AssignedCourse assignment,
                      LocalDate lessonDate, Boolean present, Integer semester, Integer academicYear, String comment) {
        this.student = student;
        this.subject = subject;
        this.assignment = assignment;
        this.lessonDate = lessonDate;
        this.present = present != null ? present : true;
        this.semester = semester;
        this.academicYear = academicYear;
        this.comment = comment;
        this.createdAt = LocalDateTime.now();
    }

    public Long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }

    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public Subject getSubject() { return subject; }
    public void setSubject(Subject subject) { this.subject = subject; }

    public AssignedCourse getAssignment() { return assignment; }
    public void setAssignment(AssignedCourse assignment) { this.assignment = assignment; }

    public LocalDate getLessonDate() { return lessonDate; }
    public void setLessonDate(LocalDate lessonDate) { this.lessonDate = lessonDate; }

    public Boolean getPresent() { return present; }
    public void setPresent(Boolean present) { this.present = present != null ? present : true; }

    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }

    public Integer getAcademicYear() { return academicYear; }
    public void setAcademicYear(Integer academicYear) { this.academicYear = academicYear; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
