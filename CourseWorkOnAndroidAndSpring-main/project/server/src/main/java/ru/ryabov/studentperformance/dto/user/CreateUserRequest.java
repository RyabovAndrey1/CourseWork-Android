package ru.ryabov.studentperformance.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {
    @NotBlank(message = "Логин обязателен")
    @Size(max = 100, message = "Логин не более 100 символов")
    private String login;
    @Size(min = 6, max = 100, message = "Пароль от 6 до 100 символов")
    private String password;
    @Email(message = "Некорректный формат email")
    @Size(max = 255, message = "Email не более 255 символов")
    private String email;
    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 255, message = "Фамилия не более 255 символов")
    private String lastName;
    @NotBlank(message = "Имя обязательно")
    @Size(max = 255, message = "Имя не более 255 символов")
    private String firstName;
    @Size(max = 255, message = "Отчество не более 255 символов")
    private String middleName;
    @NotNull(message = "Роль обязательна")
    @Size(max = 50, message = "Роль не более 50 символов")
    private String role;

    public CreateUserRequest() {}
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
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
}
