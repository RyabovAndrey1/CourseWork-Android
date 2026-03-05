package ru.ryabov.studentperformance.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.ryabov.studentperformance.dto.journal.LessonRecordDto;

import java.time.LocalDate;
import java.util.List;

/**
 * Сервис журнала преподавателя: список занятий с фильтрами и пагинацией.
 */
public interface TeacherJournalService {

    /**
     * Занятия текущего преподавателя (или все для админа/деканата) с фильтрами.
     * Каждое занятие = дата, группа, предмет, тип + список студентов (присутствие, баллы).
     */
    Page<LessonRecordDto> getLessonRecords(Long teacherIdOrNullForAll,
                                           LocalDate dateFrom, LocalDate dateTo,
                                           Long filterGroupId, Long filterSubjectId, Long filterGradeTypeId,
                                           Pageable pageable);

    /** Всего занятий (без пагинации) для того же набора фильтров. */
    long countLessonRecords(Long teacherIdOrNullForAll, LocalDate dateFrom, LocalDate dateTo,
                            Long filterGroupId, Long filterSubjectId, Long filterGradeTypeId);
}
