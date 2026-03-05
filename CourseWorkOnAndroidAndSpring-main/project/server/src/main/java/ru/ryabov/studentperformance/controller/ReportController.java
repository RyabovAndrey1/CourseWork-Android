package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.EmailService;
import ru.ryabov.studentperformance.service.PushNotificationService;
import ru.ryabov.studentperformance.service.ReportService;
import org.springframework.security.access.AccessDeniedException;
import ru.ryabov.studentperformance.dto.report.ReportRecordDto;
import ru.ryabov.studentperformance.entity.ReportRecord;
import ru.ryabov.studentperformance.repository.ReportRecordRepository;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API для скачивания отчётов (Excel/PDF) по JWT — для мобильного приложения.
 */
@RestController
@RequestMapping("/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    @Autowired
    private ReportRecordRepository reportRecordRepository;

    @GetMapping("/group/{groupId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<Resource> groupExcel(@PathVariable Long groupId) {
        Resource resource = reportService.buildGroupGradesExcel(groupId);
        saveReportRecord("GROUP", groupId, null, null, null, null, "EXCEL");
        notifyReportCreated("Отчёт по группе создан (Excel)");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-group-" + groupId + ".xlsx")
                .body(resource);
    }

    @GetMapping("/group/{groupId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<Resource> groupPdf(@PathVariable Long groupId) {
        Resource resource = reportService.buildGroupGradesPdf(groupId);
        saveReportRecord("GROUP", groupId, null, null, null, null, "PDF");
        notifyReportCreated("Отчёт по группе создан (PDF)");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-group-" + groupId + ".pdf")
                .body(resource);
    }

    /** Отчёт по дисциплине (Excel/PDF). */
    @GetMapping("/subject/{subjectId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<Resource> subjectExcel(@PathVariable Long subjectId) {
        Resource resource = reportService.buildSubjectGradesExcel(subjectId);
        saveReportRecord("SUBJECT", null, subjectId, null, null, null, "EXCEL");
        notifyReportCreated("Отчёт по дисциплине создан (Excel)");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-subject-" + subjectId + ".xlsx")
                .body(resource);
    }

    @GetMapping("/subject/{subjectId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<Resource> subjectPdf(@PathVariable Long subjectId) {
        Resource resource = reportService.buildSubjectGradesPdf(subjectId);
        saveReportRecord("SUBJECT", null, subjectId, null, null, null, "PDF");
        notifyReportCreated("Отчёт по дисциплине создан (PDF)");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-subject-" + subjectId + ".pdf")
                .body(resource);
    }

    @GetMapping("/student/{studentId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<Resource> studentExcel(
            @PathVariable Long studentId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        ensureStudentCanAccessReport(studentId);
        Resource resource = (dateFrom != null || dateTo != null)
                ? reportService.buildStudentGradesExcel(studentId, dateFrom, dateTo, null)
                : reportService.buildStudentGradesExcel(studentId);
        saveReportRecord("STUDENT", null, null, studentId, dateFrom, dateTo, "EXCEL");
        notifyReportCreated("Отчёт по студенту создан (Excel)");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-student-" + studentId + ".xlsx")
                .body(resource);
    }

    @GetMapping("/student/{studentId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<Resource> studentPdf(
            @PathVariable Long studentId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        ensureStudentCanAccessReport(studentId);
        Resource resource = (dateFrom != null || dateTo != null)
                ? reportService.buildStudentGradesPdf(studentId, dateFrom, dateTo, null)
                : reportService.buildStudentGradesPdf(studentId);
        saveReportRecord("STUDENT", null, null, studentId, dateFrom, dateTo, "PDF");
        notifyReportCreated("Отчёт по студенту создан (PDF)");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-student-" + studentId + ".pdf")
                .body(resource);
    }

    private void saveReportRecord(String reportType, Long groupId, Long subjectId, Long studentId,
                                  LocalDate periodFrom, LocalDate periodTo, String format) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) return;
        ReportRecord r = new ReportRecord();
        r.setUserId(principal.getId());
        r.setReportType(reportType);
        r.setGroupId(groupId);
        r.setSubjectId(subjectId);
        r.setStudentId(studentId);
        r.setPeriodFrom(periodFrom);
        r.setPeriodTo(periodTo);
        r.setFormat(format);
        r.setCreatedAt(LocalDateTime.now());
        reportRecordRepository.save(r);
    }

    /** Список созданных отчётов текущего пользователя (для админа, деканата, преподавателя, студента). */
    @GetMapping("/records")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<List<ReportRecordDto>>> getReportRecords() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(401).body(ApiResponse.error("Не авторизован"));
        }
        List<ReportRecord> list = reportRecordRepository.findByUserIdOrderByCreatedAtDesc(principal.getId());
        List<ReportRecordDto> dtos = list.stream().map(r -> {
            ReportRecordDto dto = new ReportRecordDto();
            dto.setId(r.getId());
            dto.setReportType(r.getReportType());
            dto.setGroupId(r.getGroupId());
            dto.setSubjectId(r.getSubjectId());
            dto.setStudentId(r.getStudentId());
            dto.setPeriodFrom(r.getPeriodFrom());
            dto.setPeriodTo(r.getPeriodTo());
            dto.setFormat(r.getFormat());
            dto.setCreatedAt(r.getCreatedAt());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("OK", dtos));
    }

    private void notifyReportCreated(String body) {
        if (pushNotificationService == null || !pushNotificationService.isEnabled()) return;
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            pushNotificationService.sendToUser(principal.getId(), "Отчёт создан", body);
        }
    }

    private void ensureStudentCanAccessReport(Long studentId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal
                && "STUDENT".equals(principal.getRole())) {
            var currentStudent = studentRepository.findByUserId(principal.getId());
            if (currentStudent.isEmpty() || !currentStudent.get().getStudentId().equals(studentId)) {
                throw new AccessDeniedException("Студент может скачать только свой отчёт");
            }
        }
    }

    /** Отправить отчёт по группе на почту текущего пользователя. */
    @GetMapping("/group/{groupId}/send-excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> sendGroupReportExcel(@PathVariable Long groupId) {
        return sendReportToEmail(() -> reportService.buildGroupGradesExcel(groupId),
                "report-group-" + groupId + ".xlsx", "Отчёт по группе (Excel)");
    }

    @GetMapping("/group/{groupId}/send-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> sendGroupReportPdf(@PathVariable Long groupId) {
        return sendReportToEmail(() -> reportService.buildGroupGradesPdf(groupId),
                "report-group-" + groupId + ".pdf", "Отчёт по группе (PDF)");
    }

    @GetMapping("/subject/{subjectId}/send-excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> sendSubjectReportExcel(@PathVariable Long subjectId) {
        return sendReportToEmail(() -> reportService.buildSubjectGradesExcel(subjectId),
                "report-subject-" + subjectId + ".xlsx", "Отчёт по дисциплине (Excel)");
    }

    @GetMapping("/subject/{subjectId}/send-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<String>> sendSubjectReportPdf(@PathVariable Long subjectId) {
        return sendReportToEmail(() -> reportService.buildSubjectGradesPdf(subjectId),
                "report-subject-" + subjectId + ".pdf", "Отчёт по дисциплине (PDF)");
    }

    /** Отправить отчёт по студенту на почту текущего пользователя (опционально за период dateFrom–dateTo). */
    @GetMapping("/student/{studentId}/send-excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<String>> sendStudentReportExcel(
            @PathVariable Long studentId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        ensureStudentCanAccessReport(studentId);
        Resource res = (dateFrom != null || dateTo != null)
                ? reportService.buildStudentGradesExcel(studentId, dateFrom, dateTo, null)
                : reportService.buildStudentGradesExcel(studentId);
        return sendReportToEmail(() -> res,
                "report-student-" + studentId + ".xlsx", "Отчёт по студенту (Excel)");
    }

    @GetMapping("/student/{studentId}/send-pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<ApiResponse<String>> sendStudentReportPdf(
            @PathVariable Long studentId,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo) {
        ensureStudentCanAccessReport(studentId);
        Resource res = (dateFrom != null || dateTo != null)
                ? reportService.buildStudentGradesPdf(studentId, dateFrom, dateTo, null)
                : reportService.buildStudentGradesPdf(studentId);
        return sendReportToEmail(() -> res,
                "report-student-" + studentId + ".pdf", "Отчёт по студенту (PDF)");
    }

    private ResponseEntity<ApiResponse<String>> sendReportToEmail(java.util.function.Supplier<Resource> reportSupplier,
                                                                   String fileName, String subject) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(401).body(ApiResponse.<String>error("Не авторизован"));
        }
        String email = principal.getEmail();
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.<String>error("У пользователя не указан email. Укажите email в профиле."));
        }
        if (emailService == null) {
            return ResponseEntity.status(503).body(ApiResponse.<String>error("Отправка почты не настроена на сервере."));
        }
        try {
            Resource resource = reportSupplier.get();
            byte[] bytes;
            try (InputStream is = resource.getInputStream()) {
                bytes = is.readAllBytes();
            }
            boolean sent = emailService.sendReportByEmail(email, bytes, fileName, subject);
            if (sent) {
                notifyReportCreated("Отчёт отправлен на почту");
                return ResponseEntity.ok(ApiResponse.success("Отчёт отправлен на " + email, null));
            }
            return ResponseEntity.status(503).body(ApiResponse.<String>error("Отчёт сформирован, но не удалось отправить на почту. Проверьте настройки почты на сервере и корректность email в профиле."));
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            org.slf4j.LoggerFactory.getLogger(ReportController.class).error("Ошибка формирования отчёта: " + msg, e);
            return ResponseEntity.status(500).body(ApiResponse.<String>error("Ошибка формирования отчёта: " + msg));
        }
    }
}
