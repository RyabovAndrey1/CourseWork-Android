package ru.ryabov.studentperformance.dto.journal;

import java.math.BigDecimal;

/** Строка по одному студенту в занятии: присутствие и баллы. */
public class StudentLessonRowDto {
    private final String studentName;
    private final boolean present;
    private final BigDecimal points;

    public StudentLessonRowDto(String studentName, boolean present, BigDecimal points) {
        this.studentName = studentName;
        this.present = present;
        this.points = points != null ? points : BigDecimal.ZERO;
    }

    public String getStudentName() { return studentName; }
    public boolean isPresent() { return present; }
    public BigDecimal getPoints() { return points; }
}
