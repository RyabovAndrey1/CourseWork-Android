package ru.ryabov.studentperformance.dto.user;

public class StudentDto {

    private Long studentId;
    private Long userId;
    private String fullName;
    private String login;
    private String email;
    private String recordBookNumber;
    private String groupName;
    private Long groupId;
    private String facultyName;
    private Integer admissionYear;

    public StudentDto() {
    }

    public StudentDto(Long studentId, Long userId, String fullName, String login,
                      String email, String recordBookNumber, String groupName,
                      Long groupId, String facultyName, Integer admissionYear) {
        this.studentId = studentId;
        this.userId = userId;
        this.fullName = fullName;
        this.login = login;
        this.email = email;
        this.recordBookNumber = recordBookNumber;
        this.groupName = groupName;
        this.groupId = groupId;
        this.facultyName = facultyName;
        this.admissionYear = admissionYear;
    }

    // Getters and Setters
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRecordBookNumber() { return recordBookNumber; }
    public void setRecordBookNumber(String recordBookNumber) { this.recordBookNumber = recordBookNumber; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public Integer getAdmissionYear() { return admissionYear; }
    public void setAdmissionYear(Integer admissionYear) { this.admissionYear = admissionYear; }
}
