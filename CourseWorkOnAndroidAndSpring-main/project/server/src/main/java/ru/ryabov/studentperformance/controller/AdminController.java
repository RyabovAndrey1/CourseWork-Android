package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.entity.*;
import ru.ryabov.studentperformance.repository.*;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.service.AuditService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired(required = false)
    private AuditService auditService;

    // Статистика системы
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<SystemStats>> getSystemStats() {
        SystemStats stats = new SystemStats();
        stats.setTotalUsers(userRepository.count());
        stats.setTotalStudents(studentRepository.count());
        stats.setTotalTeachers(teacherRepository.count());
        stats.setTotalGroups(groupRepository.count());
        stats.setTotalFaculties(facultyRepository.count());
        stats.setTotalDepartments(departmentRepository.count());
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // Управление пользователями
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userRepository.findAll()));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Void>> updateUserRole(
            @PathVariable Long userId,
            @RequestParam String role) {
        return userRepository.findById(userId)
                .map(user -> {
                    try {
                        user.setRole(User.Role.valueOf(role.toUpperCase()));
                        userRepository.save(user);
                        if (auditService != null) auditService.logAction("UPDATE_ROLE", SecurityUtils.getCurrentActor(), "User", userId, "role=" + role);
                        return ResponseEntity.ok(ApiResponse.<Void>success("Роль изменена", null));
                    } catch (IllegalArgumentException e) {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.<Void>error("Недопустимая роль"));
                    }
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Пользователь не найден")));
    }

    // Управление группами
    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<StudyGroup>>> getAllGroups() {
        return ResponseEntity.ok(ApiResponse.success(groupRepository.findAllWithFaculty()));
    }

    @PostMapping("/groups")
    public ResponseEntity<ApiResponse<StudyGroup>> createGroup(@RequestBody GroupDto dto) {
        Faculty faculty = dto.getFacultyId() != null
                ? facultyRepository.findById(dto.getFacultyId()).orElse(null) : null;
        StudyGroup group = new StudyGroup(dto.getName(), faculty, dto.getAdmissionYear(), dto.getSpecialization());
        group = groupRepository.save(group);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "StudyGroup", group.getGroupId(), "name=" + group.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Группа создана", group));
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        groupRepository.deleteById(groupId);
        if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "StudyGroup", groupId, null);
        return ResponseEntity.ok(ApiResponse.<Void>success("Группа удалена", null));
    }

    // Управление факультетами
    @GetMapping("/faculties")
    public ResponseEntity<ApiResponse<List<Faculty>>> getAllFaculties() {
        return ResponseEntity.ok(ApiResponse.success(facultyRepository.findAll()));
    }

    @PostMapping("/faculties")
    public ResponseEntity<ApiResponse<Faculty>> createFaculty(@RequestBody FacultyDto dto) {
        Faculty faculty = new Faculty(dto.getName(), dto.getDeanName());
        faculty = facultyRepository.save(faculty);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Faculty", faculty.getFacultyId(), "name=" + faculty.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Факультет создан", faculty));
    }

    // Внутренний класс для статистики
    public static class SystemStats {
        private long totalUsers;
        private long totalStudents;
        private long totalTeachers;
        private long totalGroups;
        private long totalFaculties;
        private long totalDepartments;

        // Getters and Setters
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

        public long getTotalStudents() { return totalStudents; }
        public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }

        public long getTotalTeachers() { return totalTeachers; }
        public void setTotalTeachers(long totalTeachers) { this.totalTeachers = totalTeachers; }

        public long getTotalGroups() { return totalGroups; }
        public void setTotalGroups(long totalGroups) { this.totalGroups = totalGroups; }

        public long getTotalFaculties() { return totalFaculties; }
        public void setTotalFaculties(long totalFaculties) { this.totalFaculties = totalFaculties; }

        public long getTotalDepartments() { return totalDepartments; }
        public void setTotalDepartments(long totalDepartments) { this.totalDepartments = totalDepartments; }
    }

    // Вспомогательный DTO (внутренний)
    private static class GroupDto {
        private String name;
        private Long facultyId;
        private Integer admissionYear;
        private String specialization;

        public String getName() { return name; }
        public Long getFacultyId() { return facultyId; }
        public Integer getAdmissionYear() { return admissionYear; }
        public String getSpecialization() { return specialization; }
    }

    private static class FacultyDto {
        private String name;
        private String deanName;

        public String getName() { return name; }
        public String getDeanName() { return deanName; }
    }
}
