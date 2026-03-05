package ru.ryabov.studentperformance.dto.grade;

import java.math.BigDecimal;

public class GradeSummaryDto {

    private BigDecimal averageGrade;
    private int totalGrades;
    private int excellentCount;
    private int goodCount;
    private int satisfactoryCount;
    private int failCount;

    public GradeSummaryDto() {
    }

    public GradeSummaryDto(BigDecimal averageGrade, int totalGrades,
                           int excellentCount, int goodCount,
                           int satisfactoryCount, int failCount) {
        this.averageGrade = averageGrade;
        this.totalGrades = totalGrades;
        this.excellentCount = excellentCount;
        this.goodCount = goodCount;
        this.satisfactoryCount = satisfactoryCount;
        this.failCount = failCount;
    }

    public BigDecimal getAverageGrade() { return averageGrade; }
    public void setAverageGrade(BigDecimal averageGrade) { this.averageGrade = averageGrade; }

    public int getTotalGrades() { return totalGrades; }
    public void setTotalGrades(int totalGrades) { this.totalGrades = totalGrades; }

    public int getExcellentCount() { return excellentCount; }
    public void setExcellentCount(int excellentCount) { this.excellentCount = excellentCount; }

    public int getGoodCount() { return goodCount; }
    public void setGoodCount(int goodCount) { this.goodCount = goodCount; }

    public int getSatisfactoryCount() { return satisfactoryCount; }
    public void setSatisfactoryCount(int satisfactoryCount) { this.satisfactoryCount = satisfactoryCount; }

    public int getFailCount() { return failCount; }
    public void setFailCount(int failCount) { this.failCount = failCount; }
}
