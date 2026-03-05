package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.journal.AssignedCourseItemDto;
import ru.ryabov.studentperformance.entity.AssignedCourse;
import ru.ryabov.studentperformance.repository.AssignedCourseRepository;
import ru.ryabov.studentperformance.repository.TeacherRepository;
import ru.ryabov.studentperformance.security.UserPrincipal;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/assigned-courses")
public class AssignedCourseController {

    @Autowired
    private AssignedCourseRepository assignedCourseRepository;
    @Autowired
    private TeacherRepository teacherRepository;

    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<List<AssignedCourseItemDto>>> getMyAssignments() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Не авторизован"));
        }
        var teacherOpt = teacherRepository.findByUserId(principal.getId());
        if (teacherOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(List.of()));
        }
        List<AssignedCourseItemDto> list = assignedCourseRepository.findByTeacherTeacherId(teacherOpt.get().getTeacherId()).stream()
                .map(this::toDto)
                .sorted(Comparator.comparing(AssignedCourseItemDto::getSubjectName, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(AssignedCourseItemDto::getGroupName, Comparator.nullsFirst(String::compareTo)))
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    private AssignedCourseItemDto toDto(AssignedCourse ac) {
        Long subjectId = ac.getSubject() != null ? ac.getSubject().getSubjectId() : null;
        String subjectName = ac.getSubject() != null ? ac.getSubject().getName() : null;
        Long groupId = ac.getGroup() != null ? ac.getGroup().getGroupId() : null;
        String groupName = ac.getGroup() != null ? ac.getGroup().getName() : null;
        return new AssignedCourseItemDto(
                ac.getAssignmentId(),
                subjectId,
                subjectName,
                groupId,
                groupName,
                ac.getAcademicYear(),
                ac.getSemester()
        );
    }
}
