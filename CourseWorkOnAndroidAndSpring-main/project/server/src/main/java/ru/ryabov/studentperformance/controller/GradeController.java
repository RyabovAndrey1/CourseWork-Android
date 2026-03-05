package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.grade.CreateGradeRequest;
import ru.ryabov.studentperformance.dto.grade.GradeDto;
import ru.ryabov.studentperformance.dto.grade.GradeSummaryDto;
import ru.ryabov.studentperformance.dto.grade.JournalEntryDto;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.GradeService;
import ru.ryabov.studentperformance.service.JournalService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/grades")
public class GradeController {

    @Autowired
    private GradeService gradeService;
    @Autowired
    private JournalService journalService;

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER') or #studentId == authentication.principal.id")
    public ResponseEntity<ApiResponse<List<GradeDto>>> getGradesByStudent(@PathVariable Long studentId) {
        try {
            List<GradeDto> grades = gradeService.getGradesByStudentId(studentId);
            return ResponseEntity.ok(ApiResponse.success(grades));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<GradeDto>>error(e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}/subject/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER') or #studentId == authentication.principal.id")
    public ResponseEntity<ApiResponse<List<GradeDto>>> getGradesByStudentAndSubject(
            @PathVariable Long studentId,
            @PathVariable Long subjectId) {
        try {
            List<GradeDto> grades = gradeService.getGradesByStudentIdAndSubject(studentId, subjectId);
            return ResponseEntity.ok(ApiResponse.success(grades));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<GradeDto>>error(e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER') or #studentId == authentication.principal.id")
    public ResponseEntity<ApiResponse<GradeSummaryDto>> getStudentGradeSummary(@PathVariable Long studentId) {
        try {
            GradeSummaryDto summary = gradeService.getStudentGradeSummary(studentId);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<GradeSummaryDto>error(e.getMessage()));
        }
    }

    @GetMapping("/student/{studentId}/average")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER') or #studentId == authentication.principal.id")
    public ResponseEntity<ApiResponse<BigDecimal>> getStudentAverage(@PathVariable Long studentId) {
        try {
            BigDecimal average = gradeService.calculateAverageGrade(studentId);
            return ResponseEntity.ok(ApiResponse.success(average));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<BigDecimal>error(e.getMessage()));
        }
    }

    @GetMapping("/journal-entries")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<JournalEntryDto>>> getJournalEntries(
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String searchStudent,
            org.springframework.security.core.Authentication auth) {
        Long userId = auth != null && auth.getPrincipal() instanceof UserPrincipal p ? p.getId() : null;
        int y = LocalDate.now().getYear();
        int m = LocalDate.now().getMonthValue();
        LocalDate from = dateFrom != null && !dateFrom.isBlank() ? LocalDate.parse(dateFrom) : (m <= 6 ? LocalDate.of(y, 1, 1) : LocalDate.of(y, 9, 1));
        LocalDate to = dateTo != null && !dateTo.isBlank() ? LocalDate.parse(dateTo) : LocalDate.now();
        List<JournalEntryDto> list = journalService.getJournalEntries(userId, from, to, groupId, subjectId, searchStudent);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/group/{groupId}/subject/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<GradeDto>>> getGradesByGroupAndSubject(
            @PathVariable Long groupId,
            @PathVariable Long subjectId) {
        try {
            List<GradeDto> grades = gradeService.getGradesByGroupAndSubject(groupId, subjectId);
            return ResponseEntity.ok(ApiResponse.success(grades));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<GradeDto>>error(e.getMessage()));
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<GradeDto>> createGrade(@Valid @RequestBody CreateGradeRequest request) {
        try {
            GradeDto grade = gradeService.createGrade(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Оценка создана", grade));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<GradeDto>error(e.getMessage()));
        }
    }

    @PutMapping("/{gradeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<GradeDto>> updateGrade(
            @PathVariable Long gradeId,
            @Valid @RequestBody CreateGradeRequest request) {
        try {
            GradeDto grade = gradeService.updateGrade(gradeId, request);
            return ResponseEntity.ok(ApiResponse.success("Оценка обновлена", grade));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<GradeDto>error(e.getMessage()));
        }
    }

    @DeleteMapping("/{gradeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteGrade(@PathVariable Long gradeId) {
        try {
            gradeService.deleteGrade(gradeId);
            return ResponseEntity.ok(ApiResponse.<Void>success("Оценка удалена", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>error(e.getMessage()));
        }
    }
}
