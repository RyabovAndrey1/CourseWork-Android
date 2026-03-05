package ru.ryabov.studentperformance.dto.subject;

import java.math.BigDecimal;

public class SubjectDto {

    private Long subjectId;
    private String name;
    private String code;
    private BigDecimal credits;
    private Integer totalHours;
    private Integer lectureHours;
    private Integer practiceHours;
    private Integer labHours;
    private String controlType;
    private String description;

    public SubjectDto() {
    }

    public SubjectDto(Long subjectId, String name, String code, BigDecimal credits,
                      Integer totalHours, Integer lectureHours, Integer practiceHours,
                      Integer labHours, String controlType, String description) {
        this.subjectId = subjectId;
        this.name = name;
        this.code = code;
        this.credits = credits;
        this.totalHours = totalHours;
        this.lectureHours = lectureHours;
        this.practiceHours = practiceHours;
        this.labHours = labHours;
        this.controlType = controlType;
        this.description = description;
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

    public String getControlType() { return controlType; }
    public void setControlType(String controlType) { this.controlType = controlType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
