package ru.ryabov.studentperformance.service;

import ru.ryabov.studentperformance.dto.schedule.ScheduleItemDto;

import java.util.List;

/**
 * Расписание студента: даты занятий, дисциплины, посещаемость и баллы.
 */
public interface StudentScheduleService {

    /**
     * Все записи расписания для студента (по дате убывание): дата, дисциплина, был/не был, баллы за занятие.
     */
    List<ScheduleItemDto> getScheduleForStudent(Long studentId);
}
