package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.group.GroupDto;
import ru.ryabov.studentperformance.entity.Faculty;
import ru.ryabov.studentperformance.entity.StudyGroup;
import ru.ryabov.studentperformance.repository.FacultyRepository;
import ru.ryabov.studentperformance.repository.GroupRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.service.AuditService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/groups")
public class GroupController {

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private FacultyRepository facultyRepository;

    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<GroupDto>>> getAllGroups() {
        List<GroupDto> groups = groupRepository.findAllWithFaculty().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/{groupId}")
    public ResponseEntity<ApiResponse<GroupDto>> getGroupById(@PathVariable Long groupId) {
        return groupRepository.findByIdWithFaculty(groupId)
                .map(group -> ResponseEntity.ok(ApiResponse.success(toDto(group))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<GroupDto>error("Группа не найдена")));
    }

    @GetMapping("/by-faculty/{facultyId}")
    public ResponseEntity<ApiResponse<List<GroupDto>>> getGroupsByFaculty(@PathVariable Long facultyId) {
        List<GroupDto> groups = groupRepository.findByFacultyIdOrderByName(facultyId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @GetMapping("/by-year/{year}")
    public ResponseEntity<ApiResponse<List<GroupDto>>> getGroupsByYear(@PathVariable Integer year) {
        List<GroupDto> groups = groupRepository.findByAdmissionYearOrderByName(year).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(groups));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<GroupDto>> createGroup(@Valid @RequestBody GroupDto dto) {
        try {
            Faculty faculty = null;
            if (dto.getFacultyId() != null) {
                faculty = facultyRepository.findById(dto.getFacultyId()).orElse(null);
            }

            StudyGroup group = new StudyGroup(
                    dto.getName(),
                    faculty,
                    dto.getAdmissionYear(),
                    dto.getSpecialization()
            );
            group = groupRepository.save(group);
            if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "StudyGroup", group.getGroupId(), "name=" + group.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Группа создана", toDto(group)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<GroupDto>error(e.getMessage()));
        }
    }

    @PutMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<GroupDto>> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupDto dto) {
        return groupRepository.findById(groupId)
                .map(group -> {
                    group.setName(dto.getName());
                    group.setAdmissionYear(dto.getAdmissionYear());
                    group.setSpecialization(dto.getSpecialization());
                    if (dto.getFacultyId() != null) {
                        Faculty faculty = facultyRepository.findById(dto.getFacultyId()).orElse(null);
                        group.setFaculty(faculty);
                    }
                    StudyGroup saved = groupRepository.save(group);
                    if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "StudyGroup", groupId, null);
                    return ResponseEntity.ok(ApiResponse.success("Группа обновлена", toDto(saved)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<GroupDto>error("Группа не найдена")));
    }

    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGroup(@PathVariable Long groupId) {
        if (groupRepository.existsById(groupId)) {
            groupRepository.deleteById(groupId);
            if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "StudyGroup", groupId, null);
            return ResponseEntity.ok(ApiResponse.<Void>success("Группа удалена", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Группа не найдена"));
    }

    private GroupDto toDto(StudyGroup group) {
        return new GroupDto(
                group.getGroupId(),
                group.getName(),
                group.getFaculty() != null ? group.getFaculty().getName() : null,
                group.getFaculty() != null ? group.getFaculty().getFacultyId() : null,
                group.getAdmissionYear(),
                group.getSpecialization(),
                (int) groupRepository.countStudentsByGroupId(group.getGroupId())
        );
    }
}