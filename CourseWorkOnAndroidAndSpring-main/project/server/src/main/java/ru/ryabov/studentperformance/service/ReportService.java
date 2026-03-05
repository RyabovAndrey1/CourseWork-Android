package ru.ryabov.studentperformance.service;

import org.springframework.core.io.Resource;

import java.time.LocalDate;

/**
 * Формирование отчётов в Excel и PDF (требование курсовой).
 */
public interface ReportService {

    Resource buildGroupGradesExcel(Long groupId);

    Resource buildGroupGradesPdf(Long groupId);

    Resource buildStudentGradesExcel(Long studentId);

    Resource buildStudentGradesPdf(Long studentId);

    /** Отчёт по студенту с фильтром по периоду и предмету (null = без фильтра). */
    Resource buildStudentGradesExcel(Long studentId, LocalDate periodFrom, LocalDate periodTo, Long subjectId);

    Resource buildStudentGradesPdf(Long studentId, LocalDate periodFrom, LocalDate periodTo, Long subjectId);

    Resource buildSubjectGradesExcel(Long subjectId);

    Resource buildSubjectGradesPdf(Long subjectId);
}
