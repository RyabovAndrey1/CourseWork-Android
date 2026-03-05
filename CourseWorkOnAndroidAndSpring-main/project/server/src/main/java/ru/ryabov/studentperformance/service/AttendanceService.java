package ru.ryabov.studentperformance.service;

import ru.ryabov.studentperformance.entity.Attendance;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {

    Attendance mark(Long studentId, Long subjectId, Long assignmentId, LocalDate lessonDate, boolean present, Integer semester, Integer academicYear, String comment);

    List<Attendance> getByStudentAndSubject(Long studentId, Long subjectId);

    List<Attendance> getByGroupAndSubject(Long groupId, Long subjectId);

    int countPresentByStudentAndSubject(Long studentId, Long subjectId);

    int countTotalByStudentAndSubject(Long studentId, Long subjectId);

    void delete(Long attendanceId);
}
