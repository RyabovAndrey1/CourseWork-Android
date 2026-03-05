package ru.ryabov.studentperformance.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "faculties")
public class Faculty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faculty_id")
    private Long facultyId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "dean_name", length = 150)
    private String deanName;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "faculty")
    private List<Department> departments = new ArrayList<>();

    @OneToMany(mappedBy = "faculty")
    private List<StudyGroup> groups = new ArrayList<>();

    public Faculty() {
    }

    public Faculty(String name, String deanName) {
        this.name = name;
        this.deanName = deanName;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDeanName() { return deanName; }
    public void setDeanName(String deanName) { this.deanName = deanName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Department> getDepartments() { return departments; }
    public void setDepartments(List<Department> departments) { this.departments = departments; }

    public List<StudyGroup> getGroups() { return groups; }
    public void setGroups(List<StudyGroup> groups) { this.groups = groups; }
}