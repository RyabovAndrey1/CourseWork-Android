package ru.ryabov.studentperformance.dto.user;

public class UserDto {

    private Long userId;
    private String login;
    private String email;
    private String lastName;
    private String firstName;
    private String middleName;
    private String role;
    private Boolean isActive;

    public UserDto() {
    }

    public UserDto(Long userId, String login, String email, String lastName,
                   String firstName, String middleName, String role, Boolean isActive) {
        this.userId = userId;
        this.login = login;
        this.email = email;
        this.lastName = lastName;
        this.firstName = firstName;
        this.middleName = middleName;
        this.role = role;
        this.isActive = isActive;
    }

    // Getters and Setters
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
