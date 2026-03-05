package ru.ryabov.studentperformance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.studentperformance.entity.AssignedCourse;
import ru.ryabov.studentperformance.entity.Attendance;
import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.entity.Subject;
import ru.ryabov.studentperformance.repository.AssignedCourseRepository;
import ru.ryabov.studentperformance.repository.AttendanceRepository;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.repository.SubjectRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.service.AuditService;
import ru.ryabov.studentperformance.service.AttendanceService;

import java.time.LocalDate;
import java.util.List;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private AssignedCourseRepository assignedCourseRepository;

    @Autowired(required = false)
    private AuditService auditService;

    @Override
    @Transactional
    public Attendance mark(Long studentId, Long subjectId, Long assignmentId, LocalDate lessonDate, boolean present, Integer semester, Integer academicYear, String comment) {
        Student student = studentRepository.findById(studentId).orElseThrow(() -> new RuntimeException("Студент не найден"));
        Subject subject = subjectRepository.findById(subjectId).orElseThrow(() -> new RuntimeException("Дисциплина не найдена"));
        AssignedCourse assignment = assignmentId != null ? assignedCourseRepository.findById(assignmentId).orElse(null) : null;

        Attendance a = attendanceRepository.findByStudentAndSubjectAndDate(studentId, subjectId, lessonDate)
                .orElse(new Attendance(student, subject, assignment, lessonDate, present, semester, academicYear, comment));
        a.setPresent(present);
        a.setComment(comment);
        a.setSemester(semester);
        a.setAcademicYear(academicYear);
        Attendance saved = attendanceRepository.save(a);
        if (auditService != null) {
            String action = a.getAttendanceId() == null ? "CREATE" : "UPDATE";
            auditService.logAction(action, SecurityUtils.getCurrentActor(), "Attendance", saved.getAttendanceId(),
                    "studentId=" + studentId + " subjectId=" + subjectId + " date=" + lessonDate + " present=" + present);
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> getByStudentAndSubject(Long studentId, Long subjectId) {
        return attendanceRepository.findByStudentStudentIdAndSubjectSubjectIdOrderByLessonDateDesc(studentId, subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> getByGroupAndSubject(Long groupId, Long subjectId) {
        return attendanceRepository.findByGroupAndSubjectOrderByDate(groupId, subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countPresentByStudentAndSubject(Long studentId, Long subjectId) {
        return (int) attendanceRepository.countPresentByStudentAndSubject(studentId, subjectId);
    }

    @Override
    @Transactional(readOnly = true)
    public int countTotalByStudentAndSubject(Long studentId, Long subjectId) {
        return (int) attendanceRepository.countTotalByStudentAndSubject(studentId, subjectId);
    }

    @Override
    @Transactional
    public void delete(Long attendanceId) {
        if (!attendanceRepository.existsById(attendanceId)) {
            throw new RuntimeException("Запись посещаемости не найдена");
        }
        attendanceRepository.deleteById(attendanceId);
        if (auditService != null) {
            auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Attendance", attendanceId, null);
        }
    }
}
