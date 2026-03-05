package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.user.TeacherDto;
import ru.ryabov.studentperformance.entity.Department;
import ru.ryabov.studentperformance.entity.Teacher;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.DepartmentRepository;
import ru.ryabov.studentperformance.repository.TeacherRepository;
import ru.ryabov.studentperformance.repository.UserRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.service.AuditService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API для CRUD преподавателей (для мобильного приложения).
 */
@RestController
@RequestMapping("/teachers")
public class TeacherController {

    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<List<TeacherDto>>> getAllTeachers() {
        List<TeacherDto> list = teacherRepository.findAllWithUser().stream()
                .map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<TeacherDto>> getTeacherById(@PathVariable Long teacherId) {
        return teacherRepository.findByIdWithUserAndDepartment(teacherId)
                .map(t -> ResponseEntity.ok(ApiResponse.success(toDto(t))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Преподаватель не найден")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<TeacherDto>> createTeacher(@Valid @RequestBody TeacherDto dto) {
        if (dto.getUserId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("userId обязателен"));
        }
        if (teacherRepository.findByUserId(dto.getUserId()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("У этого пользователя уже есть запись преподавателя"));
        }
        User user = userRepository.findById(dto.getUserId()).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Пользователь не найден"));
        }
        Department department = dto.getDepartmentId() != null ? departmentRepository.findById(dto.getDepartmentId()).orElse(null) : null;
        Teacher t = new Teacher(user, department, dto.getAcademicDegree(), dto.getPosition());
        t = teacherRepository.save(t);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Teacher", t.getTeacherId(), "userId=" + dto.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Преподаватель создан", toDto(t)));
    }

    @PutMapping("/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<TeacherDto>> updateTeacher(
            @PathVariable Long teacherId,
            @Valid @RequestBody TeacherDto dto) {
        return teacherRepository.findById(teacherId)
                .map(t -> {
                    t.setDepartment(dto.getDepartmentId() != null ? departmentRepository.findById(dto.getDepartmentId()).orElse(null) : null);
                    t.setAcademicDegree(dto.getAcademicDegree());
                    t.setPosition(dto.getPosition());
                    Teacher saved = teacherRepository.save(t);
                    if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Teacher", teacherId, null);
                    return ResponseEntity.ok(ApiResponse.success("Преподаватель обновлён", toDto(saved)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Преподаватель не найден")));
    }

    @DeleteMapping("/{teacherId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<Void>> deleteTeacher(@PathVariable Long teacherId) {
        if (!teacherRepository.existsById(teacherId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Преподаватель не найден"));
        }
        teacherRepository.deleteById(teacherId);
        if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Teacher", teacherId, null);
        return ResponseEntity.ok(ApiResponse.success("Запись преподавателя удалена", null));
    }

    private TeacherDto toDto(Teacher t) {
        User u = t.getUser();
        var d = t.getDepartment();
        return new TeacherDto(
                t.getTeacherId(),
                u != null ? u.getUserId() : null,
                t.getFullName(),
                u != null ? u.getLogin() : null,
                u != null ? u.getEmail() : null,
                t.getAcademicDegree(),
                t.getPosition(),
                d != null ? d.getName() : null,
                d != null && d.getFaculty() != null ? d.getFaculty().getName() : null,
                d != null ? d.getDepartmentId() : null
        );
    }
}
