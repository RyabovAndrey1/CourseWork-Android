package ru.ryabov.studentperformance.dto.journal;

import java.time.LocalDate;
import java.util.List;

/** Одно занятие в журнале: дата, группа, предмет, тип, список студентов (присутствие + баллы). Id — для ссылки «Редактировать». */
public class LessonRecordDto {
    private final LocalDate date;
    private final String groupName;
    private final String subjectName;
    private final String gradeTypeName;
    private final List<StudentLessonRowDto> students;
    private final Long groupId;
    private final Long subjectId;
    private final Long gradeTypeId;

    public LessonRecordDto(LocalDate date, String groupName, String subjectName, String gradeTypeName, List<StudentLessonRowDto> students) {
        this(date, groupName, subjectName, gradeTypeName, students, null, null, null);
    }

    public LessonRecordDto(LocalDate date, String groupName, String subjectName, String gradeTypeName, List<StudentLessonRowDto> students, Long groupId, Long subjectId, Long gradeTypeId) {
        this.date = date;
        this.groupName = groupName;
        this.subjectName = subjectName;
        this.gradeTypeName = gradeTypeName;
        this.students = students != null ? students : List.of();
        this.groupId = groupId;
        this.subjectId = subjectId;
        this.gradeTypeId = gradeTypeId;
    }

    public LocalDate getDate() { return date; }
    public String getGroupName() { return groupName; }
    public String getSubjectName() { return subjectName; }
    public String getGradeTypeName() { return gradeTypeName; }
    public List<StudentLessonRowDto> getStudents() { return students; }
    public Long getGroupId() { return groupId; }
    public Long getSubjectId() { return subjectId; }
    public Long getGradeTypeId() { return gradeTypeId; }
}
