package ru.ryabov.studentperformance.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.ryabov.studentperformance.service.AuditService;

/**
 * Реализация аудита: запись в лог входа, смены пароля и важных действий.
 * Для хранения в БД можно добавить AuditLog entity и писать туда же.
 */
@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger("AUDIT");

    @Override
    public void logLoginSuccess(String login, String remoteAddr) {
        log.info("AUDIT | LOGIN_SUCCESS | login={} | ip={}", login, remoteAddr);
    }

    @Override
    public void logLoginFailure(String login, String reason, String remoteAddr) {
        log.warn("AUDIT | LOGIN_FAILURE | login={} | reason={} | ip={}", login, reason, remoteAddr);
    }

    @Override
    public void logChangePassword(String login, boolean success, String remoteAddr) {
        if (success) {
            log.info("AUDIT | CHANGE_PASSWORD | login={} | success=true | ip={}", login, remoteAddr);
        } else {
            log.warn("AUDIT | CHANGE_PASSWORD | login={} | success=false | ip={}", login, remoteAddr);
        }
    }

    @Override
    public void logCreateGrade(Long userId, Long studentId, Long subjectId, Object gradeValue) {
        log.info("AUDIT | CREATE_GRADE | userId={} | studentId={} | subjectId={} | value={}", userId, studentId, subjectId, gradeValue);
    }

    @Override
    public void logLogout(String login) {
        log.info("AUDIT | LOGOUT | login={}", login);
    }

    @Override
    public void logAction(String action, String actor, String entityType, Object entityId, String details) {
        String d = details != null && !details.isEmpty() ? " | " + details : "";
        log.info("AUDIT | {} | actor={} | entity={} | id={}{}", action, actor, entityType, entityId, d);
    }
}
