package ru.ryabov.studentperformance.dto.auth;

public class AuthResponse {

    private String token;
    private Long userId;
    private String login;
    private String email;
    private String fullName;
    private String role;

    public AuthResponse() {
    }

    public AuthResponse(String token, Long userId, String login, String email, String fullName, String role) {
        this.token = token;
        this.userId = userId;
        this.login = login;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
