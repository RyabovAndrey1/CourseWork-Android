package ru.ryabov.studentperformance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.studentperformance.dto.schedule.ScheduleItemDto;
import ru.ryabov.studentperformance.entity.Attendance;
import ru.ryabov.studentperformance.entity.Grade;
import ru.ryabov.studentperformance.repository.AttendanceRepository;
import ru.ryabov.studentperformance.repository.GradeRepository;
import ru.ryabov.studentperformance.service.StudentScheduleService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentScheduleServiceImpl implements StudentScheduleService {

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ScheduleItemDto> getScheduleForStudent(Long studentId) {
        List<Attendance> attendances = attendanceRepository.findByStudentStudentIdOrderByLessonDateDescWithDetails(studentId);
        List<Grade> grades = gradeRepository.findByStudentIdOrderByDateDescWithSubject(studentId);

        List<ScheduleItemDto> result = new ArrayList<>();
        for (Attendance a : attendances) {
            if (a.getSubject() == null) continue;
            BigDecimal value = null;
            String typeName = null;
            for (Grade g : grades) {
                if (g.getSubject() != null && g.getSubject().getSubjectId().equals(a.getSubject().getSubjectId())
                        && g.getGradeDate() != null && g.getGradeDate().equals(a.getLessonDate())) {
                    value = g.getGradeValue();
                    typeName = g.getGradeType() != null ? g.getGradeType().getName() : g.getWorkType();
                    break;
                }
            }
            result.add(new ScheduleItemDto(
                    a.getLessonDate(),
                    a.getSubject().getName(),
                    a.getSubject().getSubjectId(),
                    Boolean.TRUE.equals(a.getPresent()),
                    value,
                    typeName
            ));
        }
        return result;
    }
}
