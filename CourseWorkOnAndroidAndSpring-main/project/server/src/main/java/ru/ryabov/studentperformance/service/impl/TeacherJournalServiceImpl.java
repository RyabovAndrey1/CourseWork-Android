package ru.ryabov.studentperformance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.studentperformance.dto.journal.LessonRecordDto;
import ru.ryabov.studentperformance.dto.journal.StudentLessonRowDto;
import ru.ryabov.studentperformance.entity.*;
import ru.ryabov.studentperformance.repository.*;
import ru.ryabov.studentperformance.service.TeacherJournalService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TeacherJournalServiceImpl implements TeacherJournalService {

    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private AssignedCourseRepository assignedCourseRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<LessonRecordDto> getLessonRecords(Long teacherIdOrNullForAll, LocalDate dateFrom, LocalDate dateTo,
                                                  Long filterGroupId, Long filterSubjectId, Long filterGradeTypeId,
                                                  Pageable pageable) {
        List<Long> assignmentIds = resolveAssignmentIds(teacherIdOrNullForAll);
        if (assignmentIds.isEmpty() || dateFrom == null || dateTo == null) {
            return new PageImpl<>(List.of(), pageable, 0);
        }
        List<Grade> grades = gradeRepository.findByAssignmentIdInAndGradeDateBetween(assignmentIds, dateFrom, dateTo);
        List<Attendance> attendances = attendanceRepository.findByAssignmentIdInAndLessonDateBetween(assignmentIds, dateFrom, dateTo);

        Map<String, Boolean> presentMap = new HashMap<>();
        for (Attendance a : attendances) {
            if (a.getLessonDate() != null && a.getSubject() != null && a.getStudent() != null) {
                presentMap.put(key(a.getLessonDate(), a.getSubject().getSubjectId(), a.getStudent().getStudentId()), Boolean.TRUE.equals(a.getPresent()));
            }
        }

        // key = date + groupId + subjectId + typeId
        Map<String, List<Grade>> byLesson = new LinkedHashMap<>();
        for (Grade g : grades) {
            Long groupId = getGroupId(g);
            Long subjectId = g.getSubject() != null ? g.getSubject().getSubjectId() : null;
            Long typeId = g.getGradeType() != null ? g.getGradeType().getTypeId() : null;
            if (filterGroupId != null && !filterGroupId.equals(groupId)) continue;
            if (filterSubjectId != null && !filterSubjectId.equals(subjectId)) continue;
            if (filterGradeTypeId != null && (typeId == null || !typeId.equals(filterGradeTypeId))) continue;
            String key = lessonKey(g.getGradeDate(), groupId, subjectId, typeId);
            byLesson.computeIfAbsent(key, k -> new ArrayList<>()).add(g);
        }

        List<LessonRecordDto> list = new ArrayList<>();
        for (Map.Entry<String, List<Grade>> e : byLesson.entrySet()) {
            List<Grade> lessonGrades = e.getValue();
            if (lessonGrades.isEmpty()) continue;
            Grade first = lessonGrades.get(0);
            LocalDate date = first.getGradeDate();
            Long groupId = getGroupId(first);
            Long subjectId = first.getSubject() != null ? first.getSubject().getSubjectId() : null;
            String groupName = first.getStudent() != null && first.getStudent().getGroup() != null ? first.getStudent().getGroup().getName() : "";
            if (first.getAssignment() != null && first.getAssignment().getGroup() != null) {
                groupName = first.getAssignment().getGroup().getName();
            }
            String subjectName = first.getSubject() != null ? first.getSubject().getName() : "";
            String typeName = first.getGradeType() != null ? first.getGradeType().getName() : (first.getWorkType() != null ? first.getWorkType() : "");
            Long typeId = first.getGradeType() != null ? first.getGradeType().getTypeId() : null;

            List<StudentLessonRowDto> rows = new ArrayList<>();
            for (Grade gr : lessonGrades) {
                Long sid = gr.getStudent() != null ? gr.getStudent().getStudentId() : null;
                String name = gr.getStudent() != null ? gr.getStudent().getFullName() : "";
                BigDecimal points = gr.getGradeValue();
                // Если записи посещаемости нет — считаем «был» (есть оценка за занятие или старые данные без attendance).
                boolean present = sid != null && subjectId != null && presentMap.getOrDefault(key(date, subjectId, sid), true);
                rows.add(new StudentLessonRowDto(name, present, points));
            }
            list.add(new LessonRecordDto(date, groupName, subjectName, typeName, rows, groupId, subjectId, typeId));
        }
        list.sort(Comparator.comparing(LessonRecordDto::getDate).reversed());

        int total = list.size();
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), total);
        List<LessonRecordDto> pageContent = from < total ? list.subList(from, to) : List.of();
        return new PageImpl<>(pageContent, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public long countLessonRecords(Long teacherIdOrNullForAll, LocalDate dateFrom, LocalDate dateTo,
                                   Long filterGroupId, Long filterSubjectId, Long filterGradeTypeId) {
        List<Long> assignmentIds = resolveAssignmentIds(teacherIdOrNullForAll);
        if (assignmentIds.isEmpty() || dateFrom == null || dateTo == null) return 0;
        List<Grade> grades = gradeRepository.findByAssignmentIdInAndGradeDateBetween(assignmentIds, dateFrom, dateTo);
        Set<String> keys = new HashSet<>();
        for (Grade g : grades) {
            Long groupId = getGroupId(g);
            Long subjectId = g.getSubject() != null ? g.getSubject().getSubjectId() : null;
            Long typeId = g.getGradeType() != null ? g.getGradeType().getTypeId() : null;
            if (filterGroupId != null && !filterGroupId.equals(groupId)) continue;
            if (filterSubjectId != null && !filterSubjectId.equals(subjectId)) continue;
            if (filterGradeTypeId != null && (typeId == null || !typeId.equals(filterGradeTypeId))) continue;
            keys.add(lessonKey(g.getGradeDate(), groupId, subjectId, typeId));
        }
        return keys.size();
    }

    private List<Long> resolveAssignmentIds(Long teacherIdOrNull) {
        if (teacherIdOrNull == null) {
            return assignedCourseRepository.findAll().stream().map(AssignedCourse::getAssignmentId).toList();
        }
        return assignedCourseRepository.findByTeacherTeacherId(teacherIdOrNull).stream()
                .map(AssignedCourse::getAssignmentId).toList();
    }

    private static Long getGroupId(Grade g) {
        if (g.getAssignment() != null && g.getAssignment().getGroup() != null) {
            return g.getAssignment().getGroup().getGroupId();
        }
        return g.getStudent() != null && g.getStudent().getGroup() != null ? g.getStudent().getGroup().getGroupId() : null;
    }

    private static String lessonKey(LocalDate date, Long groupId, Long subjectId, Long typeId) {
        return date + "|" + groupId + "|" + subjectId + "|" + typeId;
    }

    private static String key(LocalDate date, Long subjectId, Long studentId) {
        return date + "|" + subjectId + "|" + studentId;
    }
}
