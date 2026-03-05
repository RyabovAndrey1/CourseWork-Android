package ru.ryabov.studentperformance.service;

/**
 * Отправка уведомлений по e-mail (требование курсовой).
 */
public interface EmailService {

    /** Проверка: заданы ли host/username/password и включена ли отправка. */
    boolean isMailConfigured();

    /**
     * Отправить письмо (тема и текст в UTF-8).
     */
    void sendMail(String to, String subject, String text);

    /**
     * Уведомить о новой оценке (опционально: студенту или преподавателю).
     */
    void notifyGradeAdded(String studentEmail, String subjectName, String gradeValue);

    /**
     * Отправить отчёт файлом на почту (вложение).
     * @return true если отправлено, false если не удалось (почта не настроена, ошибка отправки).
     */
    boolean sendReportByEmail(String toEmail, byte[] fileBytes, String fileName, String subject);

    /**
     * Отправить письмо со ссылкой для сброса пароля.
     */
    void sendPasswordResetLink(String toEmail, String resetLink);

    /**
     * Уведомить о смене пароля (после успешной смены или сброса по ссылке).
     * В письме — ссылка на страницу смены пароля (если это были не вы).
     */
    void sendPasswordChangedNotification(String toEmail, String changePasswordPageLink);

    /**
     * Отправить тестовое письмо. Возвращает null при успехе, иначе текст ошибки.
     */
    String sendTestEmail(String toEmail);
}
