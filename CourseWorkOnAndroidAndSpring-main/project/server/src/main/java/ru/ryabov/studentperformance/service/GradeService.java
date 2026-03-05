package ru.ryabov.studentperformance.service;

import ru.ryabov.studentperformance.dto.grade.GradeDto;
import ru.ryabov.studentperformance.dto.grade.CreateGradeRequest;
import ru.ryabov.studentperformance.dto.grade.GradeSummaryDto;
import ru.ryabov.studentperformance.dto.grade.StudentPointsDto;

import java.math.BigDecimal;
import java.util.List;

public interface GradeService {

    /** Баллы по группе и дисциплине за семестр: за занятия (макс. 60) + экзамен/зачёт (макс. 40), итог по шкале 0–51 неуд, 52–66 — 3, 67–81 — 4, 82–100 — 5 */
    List<StudentPointsDto> getPointsByGroupAndSubject(Long groupId, Long subjectId, Integer semester, Integer academicYear);

    List<GradeDto> getGradesByStudentId(Long studentId);

    List<GradeDto> getGradesByStudentIdAndSubject(Long studentId, Long subjectId);

    List<GradeDto> getGradesByGroupAndSubject(Long groupId, Long subjectId);

    GradeDto createGrade(CreateGradeRequest request);

    GradeDto updateGrade(Long gradeId, CreateGradeRequest request);

    void deleteGrade(Long gradeId);

    GradeSummaryDto getStudentGradeSummary(Long studentId);

    BigDecimal calculateAverageGrade(Long studentId);

    BigDecimal calculateAverageBySubject(Long studentId, Long subjectId);

    List<GradeDto> getStudentGradesBySemester(Long studentId, Integer semester, Integer academicYear);

    /** Баллы одного студента по дисциплине за семестр (60+40, итог по шкале) */
    StudentPointsDto getPointsForStudentAndSubject(Long studentId, Long subjectId, Integer semester, Integer academicYear);
}
