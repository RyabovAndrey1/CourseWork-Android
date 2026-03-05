package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.journal.LessonRecordDto;
import ru.ryabov.studentperformance.dto.journal.LessonRecordsPageDto;
import ru.ryabov.studentperformance.repository.TeacherRepository;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.TeacherJournalService;

import java.time.LocalDate;
import java.util.List;

/**
 * REST API журнала: список занятий для мобильного приложения.
 * ADMIN — только просмотр; TEACHER, DEANERY — просмотр и добавление/редактирование (в приложении).
 */
@RestController
@RequestMapping("/journal")
public class JournalController {

    @Autowired
    private TeacherJournalService teacherJournalService;
    @Autowired
    private TeacherRepository teacherRepository;

    /**
     * Список занятий с фильтрами и пагинацией.
     * Для TEACHER возвращаются только его занятия; для ADMIN/DEANERY — все.
     */
    @GetMapping("/lesson-records")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<LessonRecordsPageDto>> getLessonRecords(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long gradeTypeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long teacherId = null;
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if ("TEACHER".equals(principal.getRole())) {
                teacherId = teacherRepository.findByUserId(principal.getId())
                        .map(ru.ryabov.studentperformance.entity.Teacher::getTeacherId).orElse(null);
            }
        }
        LocalDate from = dateFrom != null ? dateFrom : LocalDate.now().minusMonths(3);
        LocalDate to = dateTo != null ? dateTo : LocalDate.now();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size)));
        var lessonPage = teacherJournalService.getLessonRecords(teacherId, from, to, groupId, subjectId, gradeTypeId, pageable);
        LessonRecordsPageDto dto = new LessonRecordsPageDto(
                lessonPage.getContent(),
                lessonPage.getTotalElements(),
                lessonPage.getTotalPages(),
                lessonPage.getNumber(),
                lessonPage.getSize());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }
}
