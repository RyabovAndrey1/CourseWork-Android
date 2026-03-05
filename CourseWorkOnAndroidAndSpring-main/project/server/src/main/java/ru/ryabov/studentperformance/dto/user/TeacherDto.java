package ru.ryabov.studentperformance.dto.user;

public class TeacherDto {

    private Long teacherId;
    private Long userId;
    private String fullName;
    private String login;
    private String email;
    private String academicDegree;
    private String position;
    private String departmentName;
    private String facultyName;
    private Long departmentId;

    public TeacherDto() {
    }

    public TeacherDto(Long teacherId, Long userId, String fullName, String login,
                      String email, String academicDegree, String position,
                      String departmentName, String facultyName) {
        this(teacherId, userId, fullName, login, email, academicDegree, position, departmentName, facultyName, null);
    }

    public TeacherDto(Long teacherId, Long userId, String fullName, String login,
                      String email, String academicDegree, String position,
                      String departmentName, String facultyName, Long departmentId) {
        this.teacherId = teacherId;
        this.userId = userId;
        this.fullName = fullName;
        this.login = login;
        this.email = email;
        this.academicDegree = academicDegree;
        this.position = position;
        this.departmentName = departmentName;
        this.facultyName = facultyName;
        this.departmentId = departmentId;
    }

    // Getters and Setters
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAcademicDegree() { return academicDegree; }
    public void setAcademicDegree(String academicDegree) { this.academicDegree = academicDegree; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }

    public String getFacultyName() { return facultyName; }
    public void setFacultyName(String facultyName) { this.facultyName = facultyName; }

    public Long getDepartmentId() { return departmentId; }
    public void setDepartmentId(Long departmentId) { this.departmentId = departmentId; }
}
