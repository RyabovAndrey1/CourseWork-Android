package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.subject.SubjectDto;
import ru.ryabov.studentperformance.entity.Subject;
import ru.ryabov.studentperformance.repository.AssignedCourseRepository;
import ru.ryabov.studentperformance.repository.SubjectRepository;
import ru.ryabov.studentperformance.repository.TeacherRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AuditService;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/subjects")
public class SubjectController {

    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private AssignedCourseRepository assignedCourseRepository;

    @Autowired(required = false)
    private AuditService auditService;

    /** Дисциплины, закреплённые за текущим преподавателем (для роли TEACHER). */
    @GetMapping("/for-teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getSubjectsForTeacher() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Не авторизован"));
        }
        var teacherOpt = teacherRepository.findByUserId(principal.getId());
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        List<Subject> subjects = assignedCourseRepository.findByTeacherTeacherId(teacherOpt.get().getTeacherId()).stream()
                .map(ac -> ac.getSubject())
                .distinct()
                .sorted((a, b) -> (a.getName() != null ? a.getName() : "").compareTo(b.getName() != null ? b.getName() : ""))
                .collect(Collectors.toList());
        List<SubjectDto> dtos = subjects.stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getAllSubjects() {
        List<SubjectDto> subjects = subjectRepository.findAllOrderByName().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @GetMapping("/{subjectId}")
    public ResponseEntity<ApiResponse<SubjectDto>> getSubjectById(@PathVariable Long subjectId) {
        return subjectRepository.findById(subjectId)
                .map(subject -> ResponseEntity.ok(ApiResponse.success(toDto(subject))))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<SubjectDto>error("Дисциплина не найдена")));
    }

    @GetMapping("/by-control-type/{controlType}")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> getSubjectsByControlType(
            @PathVariable String controlType) {
        try {
            Subject.ControlType type = Subject.ControlType.valueOf(controlType.toUpperCase());
            List<SubjectDto> subjects = subjectRepository.findByControlTypeOrderByName(type).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(ApiResponse.success(subjects));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<SubjectDto>>error("Недопустимый тип контроля"));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<SubjectDto>>> searchSubjects(@RequestParam String name) {
        List<SubjectDto> subjects = subjectRepository.searchByName(name).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(subjects));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<SubjectDto>> createSubject(@RequestBody SubjectDto dto) {
        try {
            Subject subject = new Subject(
                    dto.getName(),
                    dto.getCode(),
                    dto.getCredits(),
                    dto.getTotalHours(),
                    dto.getLectureHours(),
                    dto.getPracticeHours(),
                    dto.getLabHours(),
                    dto.getControlType() != null ? Subject.ControlType.valueOf(dto.getControlType()) : null,
                    dto.getDescription()
            );
            subject = subjectRepository.save(subject);
            if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Subject", subject.getSubjectId(), "name=" + subject.getName());
            return ResponseEntity.ok(ApiResponse.success("Дисциплина создана", toDto(subject)));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<SubjectDto>error(e.getMessage()));
        }
    }

    @PutMapping("/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public ResponseEntity<ApiResponse<SubjectDto>> updateSubject(
            @PathVariable Long subjectId,
            @RequestBody SubjectDto dto) {
        return subjectRepository.findById(subjectId)
                .map(subject -> {
                    subject.setName(dto.getName());
                    subject.setCode(dto.getCode());
                    subject.setCredits(dto.getCredits());
                    subject.setTotalHours(dto.getTotalHours());
                    subject.setLectureHours(dto.getLectureHours());
                    subject.setPracticeHours(dto.getPracticeHours());
                    subject.setLabHours(dto.getLabHours());
                    if (dto.getControlType() != null) {
                        subject.setControlType(Subject.ControlType.valueOf(dto.getControlType()));
                    }
                    subject.setDescription(dto.getDescription());
                    Subject saved = subjectRepository.save(subject);
                    if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Subject", subjectId, null);
                    return ResponseEntity.ok(ApiResponse.success("Дисциплина обновлена", toDto(saved)));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<SubjectDto>error("Дисциплина не найдена")));
    }

    @DeleteMapping("/{subjectId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long subjectId) {
        if (subjectRepository.existsById(subjectId)) {
            subjectRepository.deleteById(subjectId);
            if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Subject", subjectId, null);
            return ResponseEntity.ok(ApiResponse.<Void>success("Дисциплина удалена", null));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.<Void>error("Дисциплина не найдена"));
    }

    private SubjectDto toDto(Subject subject) {
        return new SubjectDto(
                subject.getSubjectId(),
                subject.getName(),
                subject.getCode(),
                subject.getCredits(),
                subject.getTotalHours(),
                subject.getLectureHours(),
                subject.getPracticeHours(),
                subject.getLabHours(),
                subject.getControlType() != null ? subject.getControlType().name() : null,
                subject.getDescription()
        );
    }
}
