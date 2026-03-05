package ru.ryabov.studentperformance.service;

/**
 * Сервис аудита: логирование входа, смены пароля и всех важных действий с данными (как у Загребина).
 */
public interface AuditService {

    void logLoginSuccess(String login, String remoteAddr);

    void logLoginFailure(String login, String reason, String remoteAddr);

    void logChangePassword(String login, boolean success, String remoteAddr);

    void logCreateGrade(Long userId, Long studentId, Long subjectId, Object gradeValue);

    void logLogout(String login);

    /**
     * Универсальное логирование важного действия (CREATE/UPDATE/DELETE по сущностям).
     * @param action например CREATE, UPDATE, DELETE
     * @param actor логин или "system"
     * @param entityType например User, Student, Grade
     * @param entityId идентификатор сущности (может быть null)
     * @param details дополнительные данные (опционально)
     */
    void logAction(String action, String actor, String entityType, Object entityId, String details);
}
