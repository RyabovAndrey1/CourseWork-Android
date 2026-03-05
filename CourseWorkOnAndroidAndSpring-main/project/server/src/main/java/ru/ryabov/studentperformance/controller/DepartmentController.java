package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.department.DepartmentDto;
import ru.ryabov.studentperformance.entity.Department;
import ru.ryabov.studentperformance.entity.Faculty;
import ru.ryabov.studentperformance.repository.DepartmentRepository;
import ru.ryabov.studentperformance.repository.FacultyRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.service.AuditService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API для кафедр (для мобильного приложения и веб).
 */
@RestController
@RequestMapping("/departments")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentDto>>> getAllDepartments(
            @RequestParam(required = false) Long facultyId) {
        List<Department> list = facultyId != null
                ? departmentRepository.findByFacultyFacultyIdOrderByName(facultyId)
                : departmentRepository.findAllByOrderByName();
        List<DepartmentDto> dtos = list.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping("/{departmentId}")
    public ResponseEntity<ApiResponse<DepartmentDto>> getDepartmentById(@PathVariable Long departmentId) {
        return departmentRepository.findById(departmentId)
                .map(d -> ResponseEntity.ok(ApiResponse.success(toDto(d))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Кафедра не найдена")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<DepartmentDto>> createDepartment(@Valid @RequestBody DepartmentDto dto) {
        Faculty faculty = dto.getFacultyId() != null ? facultyRepository.findById(dto.getFacultyId()).orElse(null) : null;
        Department d = new Department(dto.getName(), faculty, dto.getHeadName());
        d = departmentRepository.save(d);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Department", d.getDepartmentId(), "name=" + d.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Кафедра создана", toDto(d)));
    }

    @PutMapping("/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<DepartmentDto>> updateDepartment(
            @PathVariable Long departmentId,
            @Valid @RequestBody DepartmentDto dto) {
        return departmentRepository.findById(departmentId)
                .map(d -> {
                    d.setName(dto.getName());
                    d.setFaculty(dto.getFacultyId() != null ? facultyRepository.findById(dto.getFacultyId()).orElse(null) : null);
                    d.setHeadName(dto.getHeadName());
                    Department saved = departmentRepository.save(d);
                    if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Department", departmentId, null);
                    return ResponseEntity.ok(ApiResponse.success("Кафедра обновлена", toDto(saved)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Кафедра не найдена")));
    }

    @DeleteMapping("/{departmentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable Long departmentId) {
        if (!departmentRepository.existsById(departmentId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Кафедра не найдена"));
        }
        departmentRepository.deleteById(departmentId);
        if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Department", departmentId, null);
        return ResponseEntity.ok(ApiResponse.success("Кафедра удалена", null));
    }

    private DepartmentDto toDto(Department d) {
        Faculty f = d.getFaculty();
        return new DepartmentDto(
                d.getDepartmentId(),
                d.getName(),
                f != null ? f.getFacultyId() : null,
                f != null ? f.getName() : null,
                d.getHeadName()
        );
    }
}
