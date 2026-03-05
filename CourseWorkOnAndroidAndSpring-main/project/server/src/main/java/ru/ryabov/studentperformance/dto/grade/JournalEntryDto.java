package ru.ryabov.studentperformance.dto.grade;

import java.time.LocalDate;

/**
 * Одна запись в журнале: оценка или посещаемость (для поиска и отображения списка).
 */
public class JournalEntryDto {

    private LocalDate date;
    private String studentFullName;
    private String groupName;
    private String subjectName;
    private String entryType; // "Оценка" | "Посещаемость"
    private String typeDetail;  // вид работы (лекция, лаба...) или пусто
    private String valueDisplay; // балл или "Да"/"Нет"

    public JournalEntryDto() {
    }

    public JournalEntryDto(LocalDate date, String studentFullName, String groupName, String subjectName,
                           String entryType, String typeDetail, String valueDisplay) {
        this.date = date;
        this.studentFullName = studentFullName;
        this.groupName = groupName;
        this.subjectName = subjectName;
        this.entryType = entryType;
        this.typeDetail = typeDetail != null ? typeDetail : "";
        this.valueDisplay = valueDisplay != null ? valueDisplay : "";
    }

    public static JournalEntryDto fromGrade(String studentName, String groupName, String subjectName,
                                            String gradeTypeName, String valueDisplay, LocalDate date) {
        return new JournalEntryDto(date, studentName, groupName, subjectName, "Оценка", gradeTypeName, valueDisplay);
    }

    public static JournalEntryDto fromAttendance(String studentName, String groupName, String subjectName,
                                                 boolean present, LocalDate date) {
        return new JournalEntryDto(date, studentName, groupName, subjectName, "Посещаемость", "", present ? "Да" : "Нет");
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getStudentFullName() { return studentFullName; }
    public void setStudentFullName(String studentFullName) { this.studentFullName = studentFullName; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getEntryType() { return entryType; }
    public void setEntryType(String entryType) { this.entryType = entryType; }

    public String getTypeDetail() { return typeDetail; }
    public void setTypeDetail(String typeDetail) { this.typeDetail = typeDetail; }

    public String getValueDisplay() { return valueDisplay; }
    public void setValueDisplay(String valueDisplay) { this.valueDisplay = valueDisplay; }
}
