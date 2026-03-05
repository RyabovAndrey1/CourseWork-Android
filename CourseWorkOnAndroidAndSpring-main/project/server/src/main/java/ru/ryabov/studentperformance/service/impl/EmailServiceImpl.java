package ru.ryabov.studentperformance.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ru.ryabov.studentperformance.service.EmailService;

import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;

/**
 * Реализация отправки e-mail через Spring Mail.
 * Если mail не настроен (JavaMailSender отсутствует или spring.mail не задан), отправка пропускается.
 */
@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.enabled:true}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String fromConfigured;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.username:}")
    private String mailUsername;

    @org.springframework.beans.factory.annotation.Value("${spring.mail.host:}")
    private String mailHost;

    private String from;

    @PostConstruct
    public void initFromAndLogMailStatus() {
        // При отправке через Gmail From должен совпадать с учётной записью (spring.mail.username),
        // иначе письма могут не доставляться до серверов получателя (SPF/временные ошибки у mi.vlgu.ru и др.).
        boolean isGmail = mailHost != null && mailHost.toLowerCase().contains("gmail");
        if (isGmail && mailUsername != null && !mailUsername.isBlank()) {
            from = mailUsername.trim();
        } else {
            from = (fromConfigured != null && !fromConfigured.isBlank())
                    ? fromConfigured.trim()
                    : (mailUsername != null && !mailUsername.isBlank() ? mailUsername.trim() : "noreply@student-performance.local");
        }
        if (mailSender != null && mailEnabled) {
            log.info("Почта включена (from={}): письма при входе, создании отчёта и смене пароля будут отправляться.", from);
        } else {
            log.info("Почта не настроена — письма не отправляются. Чтобы включить: в application.yml задайте spring.mail.host (например smtp.mail.ru), spring.mail.username, spring.mail.password. Для Mail.ru часто нужен «пароль приложения».");
        }
    }

    @Override
    public boolean isMailConfigured() {
        return mailEnabled && mailSender != null;
    }

    /**
     * Отправка письма одному получателю (to). Кодировка UTF-8 для корректной передачи русского текста.
     * Важно: всегда один получатель — адрес того, кому предназначено письмо (например, email вошедшего пользователя).
     */
    @Override
    public void sendMail(String to, String subject, String text) {
        if (!mailEnabled || mailSender == null || to == null || to.isBlank()) {
            if (to != null && !to.isBlank() && (mailSender == null || !mailEnabled)) {
                log.debug("Почта не настроена, письмо не отправлено: to={}", to);
            }
            return;
        }
        String fromAddr = (from != null && !from.isBlank()) ? from : mailUsername;
        if (fromAddr == null || fromAddr.isBlank()) fromAddr = "noreply@localhost";
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, false, "UTF-8");
            helper.setFrom(fromAddr);
            helper.setTo(to.trim());
            helper.setSubject(subject != null ? subject : "");
            helper.setText(text != null ? text : "", false);
            helper.setReplyTo(fromAddr);
            mailSender.send(msg);
            log.debug("Письмо отправлено на единственный адрес: {}", to.trim());
        } catch (Exception e) {
            log.warn("Ошибка отправки письма на {}: {} (полное исключение: {})", to, e.getMessage(), e);
            if (e.getCause() != null) {
                log.debug("Причина: ", e.getCause());
            }
        }
    }

    @Override
    public void notifyGradeAdded(String studentEmail, String subjectName, String gradeValue) {
        if (studentEmail == null || studentEmail.isBlank()) return;
        String subject = "Новая оценка по дисциплине";
        String text = String.format("Вам выставлена оценка по дисциплине «%s»: %s.", subjectName, gradeValue);
        sendMail(studentEmail, subject, text);
    }

    @Override
    public boolean sendReportByEmail(String toEmail, byte[] fileBytes, String fileName, String subject) {
        if (!mailEnabled || mailSender == null || toEmail == null || toEmail.isBlank() || fileBytes == null) {
            if (toEmail != null && !toEmail.isBlank()) {
                log.warn("Отчёт не отправлен на {}: почта не настроена (задайте spring.mail.host, username, password и при необходимости spring.mail.enabled=true)", toEmail);
            }
            return false;
        }
        String fromAddr = (from != null && !from.isBlank()) ? from : mailUsername;
        if (fromAddr == null || fromAddr.isBlank()) fromAddr = "noreply@localhost";
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromAddr);
            helper.setTo(toEmail);
            helper.setSubject(subject != null ? subject : "Отчёт");
            helper.setText("Во вложении отчёт.", false);
            helper.addAttachment(fileName, new org.springframework.core.io.ByteArrayResource(fileBytes));
            mailSender.send(msg);
            log.debug("Отчёт отправлен на {}", toEmail);
            return true;
        } catch (Exception e) {
            log.warn("Ошибка отправки отчёта на {}: {} (исключение: {})", toEmail, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void sendPasswordResetLink(String toEmail, String resetLink) {
        if (toEmail == null || toEmail.isBlank() || resetLink == null || resetLink.isBlank()) return;
        String subject = "Восстановление пароля — Учёт успеваемости";
        String text = "Вы запросили восстановление пароля.\n\nПерейдите по ссылке для установки нового пароля (ссылка действительна 1 час):\n\n" + resetLink + "\n\nЕсли вы не запрашивали сброс пароля, проигнорируйте это письмо — ваш пароль не изменится.\n\n— Система учёта успеваемости";
        sendMail(toEmail, subject, text);
    }

    @Override
    public void sendPasswordChangedNotification(String toEmail, String changePasswordPageLink) {
        if (toEmail == null || toEmail.isBlank()) return;
        String subject = "Пароль изменён — Учёт успеваемости";
        String text = "Пароль вашего аккаунта в системе учёта успеваемости был изменён.\n\n"
                + "Если это были не вы, перейдите по ссылке для смены пароля:\n" + (changePasswordPageLink != null ? changePasswordPageLink : "") + "\n\n"
                + "— Система учёта успеваемости";
        sendMail(toEmail, subject, text);
    }

    @Override
    public String sendTestEmail(String toEmail) {
        if (toEmail == null || toEmail.isBlank()) return "Укажите адрес почты.";
        if (!isMailConfigured()) return "Почта не настроена. Задайте в application.yml: spring.mail.host, spring.mail.username, spring.mail.password (для Mail.ru — пароль приложения).";
        try {
            String fromAddr = (from != null && !from.isBlank()) ? from : mailUsername;
            if (fromAddr == null || fromAddr.isBlank()) fromAddr = "noreply@localhost";
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddr);
            msg.setTo(toEmail.trim());
            msg.setSubject("Тест — Учёт успеваемости");
            msg.setText("Это тестовое письмо. Если вы его получили, почта настроена верно.");
            mailSender.send(msg);
            return null;
        } catch (Exception e) {
            log.warn("Ошибка тестовой отправки на {}: {}", toEmail, e.getMessage());
            return e.getMessage();
        }
    }
}
