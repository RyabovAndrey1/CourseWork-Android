package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.entity.Attendance;
import ru.ryabov.studentperformance.service.AttendanceService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @GetMapping("/group/{groupId}/subject/{subjectId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getByGroupAndSubject(
            @PathVariable Long groupId, @PathVariable Long subjectId) {
        List<Attendance> list = attendanceService.getByGroupAndSubject(groupId, subjectId);
        List<Map<String, Object>> dto = list.stream().map(a -> Map.<String, Object>of(
                "attendanceId", a.getAttendanceId(),
                "studentId", a.getStudent().getStudentId(),
                "studentName", a.getStudent().getFullName(),
                "subjectId", a.getSubject().getSubjectId(),
                "lessonDate", a.getLessonDate().toString(),
                "present", a.getPresent(),
                "semester", a.getSemester() != null ? a.getSemester() : "",
                "academicYear", a.getAcademicYear() != null ? a.getAcademicYear() : ""
        )).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dto));
    }

    @PostMapping("/mark")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> mark(
            @RequestParam Long studentId,
            @RequestParam Long subjectId,
            @RequestParam(required = false) Long assignmentId,
            @RequestParam String lessonDate,
            @RequestParam(defaultValue = "true") boolean present,
            @RequestParam(required = false) Integer semester,
            @RequestParam(required = false) Integer academicYear,
            @RequestParam(required = false) String comment) {
        LocalDate date = LocalDate.parse(lessonDate);
        Attendance a = attendanceService.mark(studentId, subjectId, assignmentId, date, present, semester, academicYear, comment);
        return ResponseEntity.ok(ApiResponse.success("Посещаемость отмечена", Map.of("attendanceId", a.getAttendanceId())));
    }

    @DeleteMapping("/{attendanceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long attendanceId) {
        attendanceService.delete(attendanceId);
        return ResponseEntity.ok(ApiResponse.success("Запись удалена", null));
    }
}
