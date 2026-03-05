package ru.ryabov.studentperformance.dto.journal;

import java.util.List;

/** Ответ API journal/lesson-records: страница занятий (для мобильного приложения). */
public class LessonRecordsPageDto {
    private final List<LessonRecordDto> content;
    private final long totalElements;
    private final int totalPages;
    private final int number;
    private final int size;

    public LessonRecordsPageDto(List<LessonRecordDto> content, long totalElements, int totalPages, int number, int size) {
        this.content = content != null ? content : List.of();
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.number = number;
        this.size = size;
    }

    public List<LessonRecordDto> getContent() { return content; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
    public int getNumber() { return number; }
    public int getSize() { return size; }
}
