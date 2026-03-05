package ru.ryabov.studentperformance.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.studentperformance.dto.grade.GradeDto;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.dto.grade.CreateGradeRequest;
import ru.ryabov.studentperformance.dto.grade.GradeSummaryDto;
import ru.ryabov.studentperformance.dto.grade.StudentPointsDto;
import ru.ryabov.studentperformance.entity.*;
import ru.ryabov.studentperformance.repository.*;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.service.AuditService;
import ru.ryabov.studentperformance.service.EmailService;
import ru.ryabov.studentperformance.service.GradeService;
import ru.ryabov.studentperformance.service.PushNotificationService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GradeServiceImpl implements GradeService {

    @Autowired
    private GradeRepository gradeRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private GradeTypeRepository gradeTypeRepository;

    @Autowired
    private AssignedCourseRepository assignedCourseRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired(required = false)
    private AuditService auditService;

    @Autowired(required = false)
    private PushNotificationService pushNotificationService;

    private static final Set<String> CLASSES_CODES = Set.of("LECTURE", "LAB", "PRACTICE", "CONTROL");
    private static final Set<String> EXAM_CREDIT_CODES = Set.of("EXAM", "CREDIT");
    private static final BigDecimal CAP_CLASSES = new BigDecimal("60");
    private static final BigDecimal CAP_EXAM = new BigDecimal("40");

    @Override
    @Transactional(readOnly = true)
    public List<StudentPointsDto> getPointsByGroupAndSubject(Long groupId, Long subjectId, Integer semester, Integer academicYear) {
        List<Grade> grades = gradeRepository.findByGroupAndSubject(groupId, subjectId);
        List<Student> students = studentRepository.findByGroupIdWithUser(groupId);
        List<StudentPointsDto> result = new ArrayList<>();
        for (Student s : students) {
            StudentPointsDto dto = computePointsForStudent(s.getStudentId(), s.getFullName(), grades, semester, academicYear);
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public StudentPointsDto getPointsForStudentAndSubject(Long studentId, Long subjectId, Integer semester, Integer academicYear) {
        List<Grade> grades = gradeRepository.findByStudentAndSubjectId(studentId, subjectId);
        Student student = studentRepository.findById(studentId).orElse(null);
        String name = student != null ? student.getFullName() : "";
        return computePointsForStudent(studentId, name, grades, semester, academicYear);
    }

    private StudentPointsDto computePointsForStudent(Long studentId, String fullName,
                                                     List<Grade> allGrades, Integer semester, Integer academicYear) {
        List<Grade> studentGrades = allGrades.stream()
                .filter(g -> g.getStudent() != null && g.getStudent().getStudentId().equals(studentId))
                .filter(g -> matchSemesterYear(g.getSemester(), g.getAcademicYear(), semester, academicYear))
                .toList();
        BigDecimal classes = BigDecimal.ZERO;
        BigDecimal examCredit = BigDecimal.ZERO;
        for (Grade g : studentGrades) {
            if (g.getGradeValue() == null) continue;
            String code = g.getGradeType() != null ? g.getGradeType().getCode() : null;
            if (code != null && CLASSES_CODES.contains(code)) {
                classes = classes.add(g.getGradeValue());
            } else if (code != null && EXAM_CREDIT_CODES.contains(code)) {
                examCredit = examCredit.add(g.getGradeValue());
            }
        }
        classes = classes.min(CAP_CLASSES);
        examCredit = examCredit.min(CAP_EXAM);
        BigDecimal total = classes.add(examCredit);
        String label = finalGradeFromTotal(total);
        return new StudentPointsDto(studentId, fullName, classes, examCredit, total, label);
    }

    private boolean matchSemesterYear(Integer gSem, Integer gYear, Integer semester, Integer academicYear) {
        boolean semOk = (semester == null && gSem == null) || (semester != null && semester.equals(gSem));
        boolean yearOk = (academicYear == null && gYear == null) || (academicYear != null && academicYear.equals(gYear));
        return semOk && yearOk;
    }

    private String finalGradeFromTotal(BigDecimal total) {
        if (total == null || total.compareTo(BigDecimal.ZERO) < 0) return "—";
        int t = total.intValue();
        if (t <= 51) return "неуд";
        if (t <= 66) return "3";
        if (t <= 81) return "4";
        return "5";
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeDto> getGradesByStudentId(Long studentId) {
        List<Grade> grades = gradeRepository.findByStudentIdOrderByDateDesc(studentId);
        return grades.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeDto> getGradesByStudentIdAndSubject(Long studentId, Long subjectId) {
        List<Grade> grades = gradeRepository.findByStudentAndSubjectId(studentId, subjectId);
        return grades.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeDto> getGradesByGroupAndSubject(Long groupId, Long subjectId) {
        List<Grade> grades = gradeRepository.findByGroupAndSubject(groupId, subjectId);
        return grades.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GradeDto createGrade(CreateGradeRequest request) {
        Student student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Студент не найден"));

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Дисциплина не найден"));

        GradeType gradeType = null;
        if (request.getGradeTypeId() != null) {
            gradeType = gradeTypeRepository.findById(request.getGradeTypeId())
                    .orElse(null);
        }

        if (gradeType != null && gradeType.getMaxScore() != null && request.getGradeValue() != null) {
            if (request.getGradeValue().compareTo(BigDecimal.valueOf(gradeType.getMaxScore())) > 0
                    || request.getGradeValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Балл для типа «" + gradeType.getName() + "» должен быть от 0 до " + gradeType.getMaxScore());
            }
        }

        if (gradeType != null && EXAM_CREDIT_CODES.contains(gradeType.getCode())) {
            List<Grade> existing = gradeRepository.findByStudentAndSubjectId(request.getStudentId(), request.getSubjectId());
            Integer sem = request.getSemester();
            Integer year = request.getAcademicYear();
            long otherExamCredit = existing.stream()
                    .filter(g -> g.getGradeType() != null && EXAM_CREDIT_CODES.contains(g.getGradeType().getCode()))
                    .filter(g -> matchSemesterYear(g.getSemester(), g.getAcademicYear(), sem, year))
                    .count();
            if (otherExamCredit > 0) {
                throw new RuntimeException("По дисциплине за семестр допускается только одна итоговая оценка (экзамен или зачёт)");
            }
        }

        AssignedCourse assignment = null;
        if (request.getAssignmentId() != null) {
            assignment = assignedCourseRepository.findById(request.getAssignmentId())
                    .orElse(null);
        }

        Grade grade = new Grade(
                student, subject, assignment, gradeType,
                request.getGradeValue(), request.getGradeDate() != null ? request.getGradeDate() : LocalDate.now(),
                request.getSemester(), request.getAcademicYear(),
                request.getComment(), request.getWorkType()
        );

        grade = gradeRepository.save(grade);
        if (auditService != null) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            Long userId = auth != null && auth.getPrincipal() instanceof UserPrincipal p ? p.getId() : null;
            auditService.logCreateGrade(userId, student.getStudentId(), subject.getSubjectId(), request.getGradeValue());
        }
        if (pushNotificationService != null && pushNotificationService.isEnabled() && student.getUser() != null) {
            Long studentUserId = student.getUser().getUserId();
            String subjectName = subject.getName();
            pushNotificationService.sendToUser(studentUserId, "Новая оценка", "Вам выставлена оценка по дисциплине «" + subjectName + "»");
        }
        if (emailService != null && student.getUser() != null && student.getUser().getEmail() != null) {
            emailService.notifyGradeAdded(
                    student.getUser().getEmail(),
                    subject.getName(),
                    request.getGradeValue() != null ? request.getGradeValue().toString() : "—"
            );
        }
        return toDto(grade);
    }

    @Override
    @Transactional
    public GradeDto updateGrade(Long gradeId, CreateGradeRequest request) {
        Grade grade = gradeRepository.findById(gradeId)
                .orElseThrow(() -> new RuntimeException("Оценка не найдена"));

        if (request.getGradeValue() != null) {
            grade.setGradeValue(request.getGradeValue());
        }
        if (request.getComment() != null) {
            grade.setComment(request.getComment());
        }
        if (request.getGradeDate() != null) {
            grade.setGradeDate(request.getGradeDate());
        }

        grade = gradeRepository.save(grade);
        if (auditService != null) {
            auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Grade", grade.getGradeId(), "value=" + request.getGradeValue());
        }
        return toDto(grade);
    }

    @Override
    @Transactional
    public void deleteGrade(Long gradeId) {
        if (!gradeRepository.existsById(gradeId)) {
            throw new RuntimeException("Оценка не найдена");
        }
        gradeRepository.deleteById(gradeId);
        if (auditService != null) {
            auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Grade", gradeId, null);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public GradeSummaryDto getStudentGradeSummary(Long studentId) {
        BigDecimal average = calculateAverageGrade(studentId);
        List<Grade> grades = gradeRepository.findByStudentIdOrderByDateDesc(studentId);

        int totalGrades = grades.size();
        int excellentCount = 0;
        int goodCount = 0;
        int satisfactoryCount = 0;
        int failCount = 0;

        for (Grade grade : grades) {
            if (grade.getGradeValue() != null) {
                double value = grade.getGradeValue().doubleValue();
                if (value >= 32) excellentCount++;
                else if (value >= 24) goodCount++;
                else if (value >= 16) satisfactoryCount++;
                else failCount++;
            }
        }

        return new GradeSummaryDto(average, totalGrades, excellentCount, goodCount, satisfactoryCount, failCount);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateAverageGrade(Long studentId) {
        BigDecimal average = gradeRepository.findAverageByStudentId(studentId);
        return average != null ? average.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateAverageBySubject(Long studentId, Long subjectId) {
        BigDecimal average = gradeRepository.findAverageByStudentAndSubject(studentId, subjectId);
        return average != null ? average.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradeDto> getStudentGradesBySemester(Long studentId, Integer semester, Integer academicYear) {
        List<Grade> grades = gradeRepository.findByStudentAndSemesterAndYear(studentId, semester, academicYear);
        return grades.stream().map(this::toDto).collect(Collectors.toList());
    }

    private GradeDto toDto(Grade grade) {
        return new GradeDto(
                grade.getGradeId(),
                grade.getStudent() != null ? grade.getStudent().getStudentId() : null,
                grade.getStudent() != null ? grade.getStudent().getFullName() : null,
                grade.getSubject() != null ? grade.getSubject().getSubjectId() : null,
                grade.getSubject() != null ? grade.getSubject().getName() : null,
                grade.getGradeType() != null ? grade.getGradeType().getName() : null,
                grade.getGradeValue(),
                grade.getGradeDate(),
                grade.getSemester(),
                grade.getAcademicYear(),
                grade.getComment(),
                grade.getWorkType()
        );
    }
}