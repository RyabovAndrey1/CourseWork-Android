package ru.ryabov.studentperformance.dto.faculty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class FacultyDto {

    private Long facultyId;

    @NotBlank(message = "Название факультета обязательно")
    @Size(max = 200)
    private String name;

    @Size(max = 150)
    private String deanName;

    public FacultyDto() {
    }

    public FacultyDto(Long facultyId, String name, String deanName) {
        this.facultyId = facultyId;
        this.name = name;
        this.deanName = deanName;
    }

    // Getters and Setters
    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDeanName() { return deanName; }
    public void setDeanName(String deanName) { this.deanName = deanName; }
}