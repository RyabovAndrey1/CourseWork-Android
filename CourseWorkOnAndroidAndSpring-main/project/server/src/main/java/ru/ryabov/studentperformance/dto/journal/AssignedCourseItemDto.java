package ru.ryabov.studentperformance.dto.journal;

public class AssignedCourseItemDto {
    private Long assignmentId;
    private Long subjectId;
    private String subjectName;
    private Long groupId;
    private String groupName;
    private Integer academicYear;
    private Integer semester;

    public AssignedCourseItemDto() {}
    public AssignedCourseItemDto(Long assignmentId, Long subjectId, String subjectName,
                                 Long groupId, String groupName, Integer academicYear, Integer semester) {
        this.assignmentId = assignmentId;
        this.subjectId = subjectId;
        this.subjectName = subjectName;
        this.groupId = groupId;
        this.groupName = groupName;
        this.academicYear = academicYear;
        this.semester = semester;
    }
    public Long getAssignmentId() { return assignmentId; }
    public void setAssignmentId(Long assignmentId) { this.assignmentId = assignmentId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public Integer getAcademicYear() { return academicYear; }
    public void setAcademicYear(Integer academicYear) { this.academicYear = academicYear; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
}
