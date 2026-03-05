package ru.ryabov.studentperformance.dto.grade;

import java.math.BigDecimal;

/**
 * Баллы студента по дисциплине за семестр: за занятия (макс. 60) + экзамен/зачёт (макс. 40) = макс. 100.
 * Итоговая оценка по шкале: 0–51 неуд, 52–66 — 3, 67–81 — 4, 82–100 — 5.
 */
public class StudentPointsDto {

    private Long studentId;
    private String studentFullName;
    private BigDecimal classesPoints;   // макс. 60
    private BigDecimal examCreditPoints; // макс. 40
    private BigDecimal totalPoints;      // макс. 100
    private String finalGradeLabel;       // неуд / 3 / 4 / 5

    public StudentPointsDto() {
    }

    public StudentPointsDto(Long studentId, String studentFullName,
                            BigDecimal classesPoints, BigDecimal examCreditPoints,
                            BigDecimal totalPoints, String finalGradeLabel) {
        this.studentId = studentId;
        this.studentFullName = studentFullName;
        this.classesPoints = classesPoints;
        this.examCreditPoints = examCreditPoints;
        this.totalPoints = totalPoints;
        this.finalGradeLabel = finalGradeLabel;
    }

    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }

    public String getStudentFullName() { return studentFullName; }
    public void setStudentFullName(String studentFullName) { this.studentFullName = studentFullName; }

    public BigDecimal getClassesPoints() { return classesPoints; }
    public void setClassesPoints(BigDecimal classesPoints) { this.classesPoints = classesPoints; }

    public BigDecimal getExamCreditPoints() { return examCreditPoints; }
    public void setExamCreditPoints(BigDecimal examCreditPoints) { this.examCreditPoints = examCreditPoints; }

    public BigDecimal getTotalPoints() { return totalPoints; }
    public void setTotalPoints(BigDecimal totalPoints) { this.totalPoints = totalPoints; }

    public String getFinalGradeLabel() { return finalGradeLabel; }
    public void setFinalGradeLabel(String finalGradeLabel) { this.finalGradeLabel = finalGradeLabel; }
}
