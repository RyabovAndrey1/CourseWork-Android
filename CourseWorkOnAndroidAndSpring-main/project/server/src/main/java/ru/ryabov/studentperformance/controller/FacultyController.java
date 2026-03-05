package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.faculty.FacultyDto;
import ru.ryabov.studentperformance.entity.Faculty;
import ru.ryabov.studentperformance.repository.FacultyRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.service.AuditService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/faculties")
public class FacultyController {

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FacultyDto>>> getAllFaculties() {
        List<FacultyDto> faculties = facultyRepository.findAllByOrderByName().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(faculties));
    }

    @GetMapping("/{facultyId}")
    public ResponseEntity<ApiResponse<FacultyDto>> getFacultyById(@PathVariable Long facultyId) {
        return facultyRepository.findById(facultyId)
                .map(faculty -> ResponseEntity.ok(ApiResponse.success(toDto(faculty))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<FacultyDto>error("Факультет не найден")));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<FacultyDto>> createFaculty(@Valid @RequestBody FacultyDto dto) {
        try {
            Faculty faculty = new Faculty(dto.getName(), dto.getDeanName());
            faculty = facultyRepository.save(faculty);
            if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Faculty", faculty.getFacultyId(), "name=" + faculty.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Факультет создан", toDto(faculty)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<FacultyDto>error(e.getMessage()));
        }
    }

    @PutMapping("/{facultyId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<FacultyDto>> updateFaculty(
            @PathVariable Long facultyId,
            @Valid @RequestBody FacultyDto dto) {
        return facultyRepository.findById(facultyId)
                .map(faculty -> {
                    faculty.setName(dto.getName());
                    faculty.setDeanName(dto.getDeanName());
                    Faculty saved = facultyRepository.save(faculty);
                    if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Faculty", facultyId, null);
                    return ResponseEntity.ok(ApiResponse.success("Факультет обновлен", toDto(saved)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<FacultyDto>error("Факультет не найден")));
    }

    @DeleteMapping("/{facultyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFaculty(@PathVariable Long facultyId) {
        if (facultyRepository.existsById(facultyId)) {
            facultyRepository.deleteById(facultyId);
            if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Faculty", facultyId, null);
            return ResponseEntity.ok(ApiResponse.<Void>success("Факультет удален", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Факультет не найден"));
    }

    private FacultyDto toDto(Faculty faculty) {
        return new FacultyDto(
                faculty.getFacultyId(),
                faculty.getName(),
                faculty.getDeanName()
        );
    }
}
