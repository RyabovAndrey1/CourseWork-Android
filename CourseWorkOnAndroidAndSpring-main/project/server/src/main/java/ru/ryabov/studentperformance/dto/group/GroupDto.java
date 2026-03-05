package ru.ryabov.studentperformance.dto.group;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class GroupDto {

    private Long groupId;

    @NotBlank(message = "Название группы обязательно")
    @Size(max = 10)
    private String name;
    private String facultyName;
    @NotNull(message = "Факультет обязателен")
    private Long facultyId;
    @NotNull(message = "Год набора обязателен")
    @Min(value = 2000, message = "Год набора не ранее 2000")
    @Max(value = 2100, message = "Год набора не позднее 2100")
    private Integer admissionYear;
    @Size(max = 200)
    private String specialization;
    private int studentCount;

    public GroupDto() {
    }

    public GroupDto(Long groupId, String name, String facultyName, Long facultyId,
                    Integer admissionYear, String specialization, int studentCount) {
        this.groupId = groupId;
        this.name = name;
        this.facultyName = facultyName;
        this.facultyId = facultyId;
        this.admissionYear = admissionYear;
        this.specialization = specialization;
        this.studentCount = studentCount;
    }

    // Getters and Setters
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public Long getFacultyId() { return facultyId; }
    public void setFacultyId(Long facultyId) { this.facultyId = facultyId; }

    public Integer getAdmissionYear() { return admissionYear; }
    public void setAdmissionYear(Integer admissionYear) { this.admissionYear = admissionYear; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public int getStudentCount() { return studentCount; }
    public void setStudentCount(int studentCount) { this.studentCount = studentCount; }
}