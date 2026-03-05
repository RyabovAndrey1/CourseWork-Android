package ru.ryabov.studentperformance.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class DepartmentDto {

    private Long departmentId;

    @NotBlank(message = "Название кафедры обязательно")
    @Size(max = 200)
    private String name;

    private Long facultyId;
    private String facultyName;

    @Size(max = 100)
    private String headName;

    public DepartmentDto() {
    }

    public DepartmentDto(Long departmentId, String name, Long facultyId, String facultyName, String headName) {
        this.departmentId = departmentId;
        this.name = name;
        this.facultyId = facultyId;
        this.facultyName = facultyName;
        this.headName = headName;
    }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public String getHeadName() { return headName; }
    public void setHeadName(String headName) { this.headName = headName; }
}
