package ru.ryabov.studentperformance.dto.report;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Элемент списка созданных отчётов (для мобильного приложения и API).
 */
public class ReportRecordDto {

    private Long id;
    private String reportType;
    private Long groupId;
    private Long subjectId;
    private Long studentId;
    private LocalDate periodFrom;
    private LocalDate periodTo;
    private String format;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }
    public Long getGroupId() { return groupId; }
    public void setGroupId(Long groupId) { this.groupId = groupId; }
    public Long getSubjectId() { return subjectId; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public LocalDate getPeriodFrom() { return periodFrom; }
    public void setPeriodFrom(LocalDate periodFrom) { this.periodFrom = periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public void setPeriodTo(LocalDate periodTo) { this.periodTo = periodTo; }
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
