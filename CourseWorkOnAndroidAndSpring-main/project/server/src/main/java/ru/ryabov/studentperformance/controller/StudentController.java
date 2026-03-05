package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.dto.user.CreateStudentRequest;
import ru.ryabov.studentperformance.dto.user.StudentDto;
import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.entity.StudyGroup;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.GroupRepository;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.repository.UserRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.service.AuditService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST CRUD для студентов (для мобильного приложения, паритет с веб-РКП).
 */
@RestController
@RequestMapping("/students")
public class StudentController {

    private static final String DEFAULT_PASSWORD = "password123";

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<StudentDto>> getCurrentStudent() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Не авторизован"));
        }
        return studentRepository.findByUserId(principal.getId())
                .map(s -> ResponseEntity.ok(ApiResponse.success(toDto(s))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Студент не найден")));
    }

    @GetMapping("/by-group/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getStudentsByGroup(@PathVariable Long groupId) {
        List<StudentDto> list = studentRepository.findByGroupIdWithUser(groupId).stream()
                .map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<StudentDto>>> getAllStudents() {
        List<StudentDto> list = studentRepository.findAll().stream()
                .map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<StudentDto>> getStudentById(@PathVariable Long studentId) {
        return studentRepository.findById(studentId)
                .map(s -> ResponseEntity.ok(ApiResponse.success(toDto(s))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Студент не найден")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<StudentDto>> createStudent(@Valid @RequestBody CreateStudentRequest req) {
        if (userRepository.findByLogin(req.getLogin()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Пользователь с таким логином уже существует"));
        }
        String rawPassword = (req.getPassword() != null && !req.getPassword().isBlank()) ? req.getPassword() : DEFAULT_PASSWORD;
        User user = new User(req.getLogin(), passwordEncoder.encode(rawPassword), req.getEmail(),
                req.getLastName(), req.getFirstName(), req.getMiddleName(), User.Role.STUDENT);
        user = userRepository.save(user);
        StudyGroup group = req.getGroupId() != null ? groupRepository.findById(req.getGroupId()).orElse(null) : null;
        Student student = new Student(user, group, req.getRecordBookNumber(), req.getAdmissionYear(), null, null, null);
        student = studentRepository.save(student);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Student", student.getStudentId(), "login=" + req.getLogin());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Студент создан", toDto(student)));
    }

    @PutMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<StudentDto>> updateStudent(
            @PathVariable Long studentId,
            @Valid @RequestBody CreateStudentRequest req) {
        return studentRepository.findById(studentId)
                .map(student -> {
                    User u = student.getUser();
                    if (u != null) {
                        u.setEmail(req.getEmail());
                        u.setLastName(req.getLastName());
                        u.setFirstName(req.getFirstName());
                        u.setMiddleName(req.getMiddleName());
                        userRepository.save(u);
                    }
                    student.setGroup(req.getGroupId() != null ? groupRepository.findById(req.getGroupId()).orElse(null) : null);
                    student.setRecordBookNumber(req.getRecordBookNumber());
                    student.setAdmissionYear(req.getAdmissionYear());
                    Student saved = studentRepository.save(student);
                    if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Student", studentId, null);
                    return ResponseEntity.ok(ApiResponse.success("Студент обновлён", toDto(saved)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Студент не найден")));
    }

    @DeleteMapping("/{studentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteStudent(@PathVariable Long studentId) {
        return studentRepository.findById(studentId)
                .map(student -> {
                    User u = student.getUser();
                    studentRepository.delete(student);
                    if (u != null) userRepository.delete(u);
                    if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Student", studentId, null);
                    return ResponseEntity.ok(ApiResponse.<Void>success("Студент удалён", null));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Студент не найден")));
    }

    private StudentDto toDto(Student s) {
        User u = s.getUser();
        return new StudentDto(
                s.getStudentId(),
                u != null ? u.getUserId() : null,
                s.getFullName(),
                u != null ? u.getLogin() : null,
                u != null ? u.getEmail() : null,
                s.getRecordBookNumber(),
                s.getGroupName(),
                s.getGroup() != null ? s.getGroup().getGroupId() : null,
                s.getGroup() != null && s.getGroup().getFaculty() != null ? s.getGroup().getFaculty().getName() : null,
                s.getAdmissionYear()
        );
    }
}
