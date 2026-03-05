package ru.ryabov.studentperformance.dto.schedule;

import java.time.LocalDate;

/**
 * Один пункт расписания студента: дата, дисциплина, присутствовал ли, баллы за занятие.
 */
public record ScheduleItemDto(
        LocalDate lessonDate,
        String subjectName,
        Long subjectId,
        boolean present,
        java.math.BigDecimal gradeValue,
        String gradeTypeName
) {}
