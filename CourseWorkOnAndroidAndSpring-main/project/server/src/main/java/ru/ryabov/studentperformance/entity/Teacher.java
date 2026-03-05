package ru.ryabov.studentperformance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teachers")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "teacher_id")
    private Long teacherId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(name = "academic_degree", length = 100)
    private String academicDegree;

    @Column(name = "position", length = 100)
    private String position;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "teacher")
    private List<AssignedCourse> assignedCourses = new ArrayList<>();

    public Teacher() {
    }

    public Teacher(User user, Department department, String academicDegree, String position) {
        this.user = user;
        this.department = department;
        this.academicDegree = academicDegree;
        this.position = position;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public String getAcademicDegree() { return academicDegree; }
    public void setAcademicDegree(String academicDegree) { this.academicDegree = academicDegree; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<AssignedCourse> getAssignedCourses() { return assignedCourses; }
    public void setAssignedCourses(List<AssignedCourse> assignedCourses) { this.assignedCourses = assignedCourses; }

    public String getFullName() {
        if (user != null) {
            return user.getFullName();
        }
        return "";
    }
}