package ru.ryabov.studentperformance.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * Запрос на обновление пользователя. Все поля опциональны — обновляются только переданные.
 * Поддерживаются русские символы в фамилии, имени, отчестве.
 */
public class UpdateUserRequest {
    @Email(message = "Некорректный формат email")
    @Size(max = 255, message = "Email не более 255 символов")
    private String email;
    @Size(max = 255, message = "Фамилия не более 255 символов")
    private String lastName;
    @Size(max = 255, message = "Имя не более 255 символов")
    private String firstName;
    @Size(max = 255, message = "Отчество не более 255 символов")
    private String middleName;
    @Size(max = 50, message = "Роль не более 50 символов")
    private String role;
    @Size(max = 100, message = "Пароль не более 100 символов (оставьте пустым, чтобы не менять)")
    private String password;

    public UpdateUserRequest() {}
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
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
