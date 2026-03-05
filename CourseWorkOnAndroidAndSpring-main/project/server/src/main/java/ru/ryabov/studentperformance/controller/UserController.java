package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.user.StudentDto;
import ru.ryabov.studentperformance.dto.user.TeacherDto;
import ru.ryabov.studentperformance.dto.user.UserDto;
import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.entity.Teacher;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.dto.user.CreateUserRequest;
import ru.ryabov.studentperformance.dto.user.UpdateUserRequest;
import ru.ryabov.studentperformance.repository.TeacherRepository;
import ru.ryabov.studentperformance.repository.UserRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.service.AuditService;
import ru.ryabov.studentperformance.service.PushNotificationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private AuditService auditService;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<List<UserDto>>> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY') or #userId == authentication.principal.id")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.success(toUserDto(user))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<UserDto>error("Пользователь не найден")));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody CreateUserRequest req) {
        if (userRepository.findByLogin(req.getLogin()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.<UserDto>error("Пользователь с таким логином уже существует"));
        }
        if (req.getEmail() != null && !req.getEmail().isBlank() && userRepository.findByEmail(req.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.<UserDto>error("Пользователь с таким email уже существует"));
        }
        String rawPassword = (req.getPassword() != null && !req.getPassword().isBlank()) ? req.getPassword() : "password123";
        User.Role role;
        try {
            role = User.Role.valueOf(req.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.<UserDto>error("Недопустимая роль"));
        }
        User user = new User(
                req.getLogin(),
                passwordEncoder.encode(rawPassword),
                req.getEmail() != null && !req.getEmail().isBlank() ? req.getEmail() : req.getLogin() + "@local",
                req.getLastName(),
                req.getFirstName(),
                req.getMiddleName() != null ? req.getMiddleName() : "",
                role
        );
        user = userRepository.save(user);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "User", user.getUserId(), "login=" + user.getLogin());
        if (pushNotificationService != null && pushNotificationService.isEnabled()) {
            pushNotificationService.sendToRole(User.Role.ADMIN, "Новый пользователь", "Создан пользователь: " + user.getLogin());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Пользователь создан", toUserDto(user)));
    }

    /** Обновление пользователя. POST используется для совместимости (PUT/PATCH могут блокироваться). */
    @PostMapping("/{userId}/update")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<UserDto>> updateUserPost(@PathVariable Long userId, @Valid @RequestBody(required = false) UpdateUserRequest req) {
        return updateUser(userId, req);
    }

    @RequestMapping(value = "/{userId}", method = {RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<ApiResponse<UserDto>> updateUser(@PathVariable Long userId, @Valid @RequestBody(required = false) UpdateUserRequest req) {
        final UpdateUserRequest request = req != null ? req : new UpdateUserRequest();
        return userRepository.findById(userId)
                .map(user -> {
                    if (request.getEmail() != null && !request.getEmail().isBlank()) {
                        var existing = userRepository.findByEmail(request.getEmail());
                        if (existing.isPresent() && !existing.get().getUserId().equals(userId)) {
                            return ResponseEntity.badRequest().body(ApiResponse.<UserDto>error("Email уже используется другим пользователем"));
                        }
                        user.setEmail(request.getEmail());
                    }
                    if (request.getLastName() != null) user.setLastName(request.getLastName());
                    if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
                    if (request.getMiddleName() != null) user.setMiddleName(request.getMiddleName());
                    if (request.getRole() != null && !request.getRole().isBlank()) {
                        try {
                            user.setRole(User.Role.valueOf(request.getRole().toUpperCase()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                    if (request.getPassword() != null && !request.getPassword().isBlank()) {
                        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                    }
                    try {
                        User savedUser = userRepository.save(user);
                        if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "User", userId, "login=" + user.getLogin());
                        return ResponseEntity.ok(ApiResponse.success("Пользователь обновлён", toUserDto(savedUser)));
                    } catch (Exception e) {
                        return ResponseEntity.badRequest().body(ApiResponse.<UserDto>error(e.getMessage() != null ? e.getMessage() : "Ошибка сохранения"));
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<UserDto>error("Пользователь не найден")));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getStudent() != null || user.getTeacher() != null) {
                        return ResponseEntity.badRequest().body(ApiResponse.<Void>error("Нельзя удалить пользователя, привязанного к студенту или преподавателю"));
                    }
                    String deletedLogin = user.getLogin();
                    userRepository.delete(user);
                    if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "User", userId, "login=" + deletedLogin);
                    if (pushNotificationService != null && pushNotificationService.isEnabled()) {
                        pushNotificationService.sendToRole(User.Role.ADMIN, "Пользователь удалён", "Удалён пользователь: " + deletedLogin);
                    }
                    return ResponseEntity.ok(ApiResponse.<Void>success("Пользователь удалён", null));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Пользователь не найден")));
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getAllStudents() {
        List<StudentDto> students = studentRepository.findAll().stream()
                .map(this::toStudentDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER') or #studentId == authentication.principal.id")
    public ResponseEntity<ApiResponse<StudentDto>> getStudentById(@PathVariable Long studentId) {
        return studentRepository.findById(studentId)
                .map(student -> ResponseEntity.ok(ApiResponse.success(toStudentDto(student))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<StudentDto>error("Студент не найден")));
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<List<TeacherDto>>> getAllTeachers() {
        List<TeacherDto> teachers = teacherRepository.findAllWithUser().stream()
                .map(this::toTeacherDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(teachers));
    }

    @GetMapping("/teachers/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY') or #teacherId == authentication.principal.id")
    public ResponseEntity<ApiResponse<TeacherDto>> getTeacherById(@PathVariable Long teacherId) {
        return teacherRepository.findByIdWithUser(teacherId)
                .map(teacher -> ResponseEntity.ok(ApiResponse.success(toTeacherDto(teacher))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<TeacherDto>error("Преподаватель не найден")));
    }

    @PutMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deactivateUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.setIsActive(false);
                    userRepository.save(user);
                    if (auditService != null) auditService.logAction("DEACTIVATE", SecurityUtils.getCurrentActor(), "User", userId, "login=" + user.getLogin());
                    return ResponseEntity.ok(ApiResponse.<Void>success("Пользователь деактивирован", null));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<Void>error("Пользователь не найден")));
    }

    @PutMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> activateUser(@PathVariable Long userId) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.setIsActive(true);
                    userRepository.save(user);
                    if (auditService != null) auditService.logAction("ACTIVATE", SecurityUtils.getCurrentActor(), "User", userId, "login=" + user.getLogin());
                    return ResponseEntity.ok(ApiResponse.<Void>success("Пользователь активирован", null));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.<Void>error("Пользователь не найден")));
    }

    private UserDto toUserDto(User user) {
        return new UserDto(
                user.getUserId(),
                user.getLogin(),
                user.getEmail(),
                user.getLastName(),
                user.getFirstName(),
                user.getMiddleName(),
                user.getRole().name(),
                user.getIsActive()
        );
    }

    private StudentDto toStudentDto(Student student) {
        User user = student.getUser();
        return new StudentDto(
                student.getStudentId(),
                user != null ? user.getUserId() : null,
                student.getFullName(),
                user != null ? user.getLogin() : null,
                user != null ? user.getEmail() : null,
                student.getRecordBookNumber(),
                student.getGroupName(),
                student.getGroup() != null ? student.getGroup().getGroupId() : null,
                student.getGroup() != null && student.getGroup().getFaculty() != null
                        ? student.getGroup().getFaculty().getName() : null,
                student.getAdmissionYear()
        );
    }

    private TeacherDto toTeacherDto(Teacher teacher) {
        User user = teacher.getUser();
        return new TeacherDto(
                teacher.getTeacherId(),
                user != null ? user.getUserId() : null,
                teacher.getFullName(),
                user != null ? user.getLogin() : null,
                user != null ? user.getEmail() : null,
                teacher.getAcademicDegree(),
                teacher.getPosition(),
                teacher.getDepartment() != null ? teacher.getDepartment().getName() : null,
                teacher.getDepartment() != null && teacher.getDepartment().getFaculty() != null
                        ? teacher.getDepartment().getFaculty().getName() : null
        );
    }
}