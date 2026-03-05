package ru.ryabov.studentperformance.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.studentperformance.dto.grade.JournalEntryDto;
import ru.ryabov.studentperformance.entity.AssignedCourse;
import ru.ryabov.studentperformance.entity.Attendance;
import ru.ryabov.studentperformance.entity.Grade;
import ru.ryabov.studentperformance.repository.AssignedCourseRepository;
import ru.ryabov.studentperformance.repository.AttendanceRepository;
import ru.ryabov.studentperformance.repository.GradeRepository;
import ru.ryabov.studentperformance.repository.TeacherRepository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Сервис записей журнала (оценки + посещаемость) с фильтрами для REST и веб.
 */
@Service
public class JournalService {

    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private AssignedCourseRepository assignedCourseRepository;
    @Autowired
    private TeacherRepository teacherRepository;

    /**
     * Идентификаторы назначений курсов для пользователя: для преподавателя — его курсы, иначе все.
     */
    public List<Long> getAssignmentIdsForUser(Long userId) {
        if (userId == null) return List.of();
        var teacherOpt = teacherRepository.findByUserId(userId);
        if (teacherOpt.isPresent()) {
            return assignedCourseRepository.findByTeacherTeacherId(teacherOpt.get().getTeacherId()).stream()
                    .map(AssignedCourse::getAssignmentId).toList();
        }
        return assignedCourseRepository.findAll().stream().map(AssignedCourse::getAssignmentId).toList();
    }

    @Transactional(readOnly = true)
    public List<JournalEntryDto> getJournalEntries(Long userId, LocalDate dateFrom, LocalDate dateTo,
                                                   Long filterGroupId, Long filterSubjectId, String searchStudent) {
        if (dateFrom == null || dateTo == null) return List.of();
        String studentLower = (searchStudent != null && !searchStudent.isBlank()) ? searchStudent.trim().toLowerCase() : null;
        List<JournalEntryDto> list = new ArrayList<>();

        List<Grade> grades = new ArrayList<>();
        List<Long> assignmentIds = getAssignmentIdsForUser(userId);
        if (!assignmentIds.isEmpty()) {
            grades.addAll(gradeRepository.findByAssignmentIdInAndGradeDateBetween(assignmentIds, dateFrom, dateTo));
        }
        if (filterGroupId != null && filterSubjectId != null) {
            for (Grade g : gradeRepository.findByGroupAndSubject(filterGroupId, filterSubjectId)) {
                if (g.getGradeDate() != null && !g.getGradeDate().isBefore(dateFrom) && !g.getGradeDate().isAfter(dateTo)
                        && g.getAssignment() == null
                        && grades.stream().noneMatch(ag -> ag.getGradeId().equals(g.getGradeId()))) {
                    grades.add(g);
                }
            }
        }
        for (Grade g : grades) {
            String studentName = g.getStudent() != null ? g.getStudent().getFullName() : "";
            String groupName = g.getStudent() != null && g.getStudent().getGroup() != null ? g.getStudent().getGroup().getName() : "";
            String subjectName = g.getSubject() != null ? g.getSubject().getName() : "";
            if (filterGroupId != null && (g.getStudent() == null || g.getStudent().getGroup() == null || !g.getStudent().getGroup().getGroupId().equals(filterGroupId))) continue;
            if (filterSubjectId != null && (g.getSubject() == null || !g.getSubject().getSubjectId().equals(filterSubjectId))) continue;
            if (studentLower != null && !studentName.toLowerCase().contains(studentLower)) continue;
            String typeName = g.getGradeType() != null ? g.getGradeType().getName() : (g.getWorkType() != null ? g.getWorkType() : "");
            String value = g.getGradeValue() != null ? g.getGradeValue().toPlainString() : "";
            list.add(JournalEntryDto.fromGrade(studentName, groupName, subjectName, typeName, value, g.getGradeDate()));
        }
        List<Attendance> attendances = new ArrayList<>();
        if (!assignmentIds.isEmpty()) {
            attendances.addAll(attendanceRepository.findByAssignmentIdInAndLessonDateBetween(assignmentIds, dateFrom, dateTo));
        }
        if (filterGroupId != null && filterSubjectId != null) {
            for (Attendance a : attendanceRepository.findByGroupAndSubjectOrderByDate(filterGroupId, filterSubjectId)) {
                if (a.getLessonDate() != null && !a.getLessonDate().isBefore(dateFrom) && !a.getLessonDate().isAfter(dateTo)
                        && a.getAssignment() == null
                        && attendances.stream().noneMatch(aa -> aa.getAttendanceId().equals(a.getAttendanceId()))) {
                    attendances.add(a);
                }
            }
        }
        for (Attendance a : attendances) {
            String studentName = a.getStudent() != null ? a.getStudent().getFullName() : "";
            String groupName = a.getStudent() != null && a.getStudent().getGroup() != null ? a.getStudent().getGroup().getName() : "";
            String subjectName = a.getSubject() != null ? a.getSubject().getName() : "";
            if (filterGroupId != null && (a.getStudent() == null || a.getStudent().getGroup() == null || !a.getStudent().getGroup().getGroupId().equals(filterGroupId))) continue;
            if (filterSubjectId != null && (a.getSubject() == null || !a.getSubject().getSubjectId().equals(filterSubjectId))) continue;
            if (studentLower != null && !studentName.toLowerCase().contains(studentLower)) continue;
            list.add(JournalEntryDto.fromAttendance(studentName, groupName, subjectName, Boolean.TRUE.equals(a.getPresent()), a.getLessonDate()));
        }
        list.sort(Comparator.comparing(JournalEntryDto::getDate).reversed().thenComparing(JournalEntryDto::getStudentFullName));
        return list;
    }
}
