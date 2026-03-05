package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.security.WebLogoutHandler;
import ru.ryabov.studentperformance.service.AuditService;
import ru.ryabov.studentperformance.service.AuthService;
import ru.ryabov.studentperformance.service.EmailService;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Контроллер веб-интерфейса (курсовая «Разработка корпоративных приложений»).
 * Единое окно входа (логин, пароль, смена пароля); после входа — панель по роли.
 */
@Controller
@RequestMapping("/web")
public class WebController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private AuditService auditService;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired
    private WebLogoutHandler webLogoutHandler;

    @Autowired
    private ru.ryabov.studentperformance.service.GradeService gradeService;

    /** Выход по ссылке из письма (GET). В форме на сайте используется POST /web/logout. */
    @GetMapping("/logout")
    public String logoutGet(HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            webLogoutHandler.logout(request, response, auth);
            new SecurityContextLogoutHandler().logout(request, response, auth);
        }
        return "redirect:/web/login?logout";
    }

    @GetMapping("/login")
    public String login(Model model, String error, String logout, String changed,
                        jakarta.servlet.http.HttpServletResponse response) {
        // После выхода браузер не должен кэшировать страницу входа — иначе при повторном входе возможен 403 из‑за устаревшего CSRF-токена
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        if (error != null) model.addAttribute("error", "Неверный логин или пароль");
        if (logout != null) model.addAttribute("message", "Вы вышли из системы");
        if (changed != null) model.addAttribute("message", "Пароль успешно изменён. Войдите с новым паролем.");
        return "web/login";
    }

    @GetMapping("/change-password")
    public String changePasswordForm() {
        return "web/change-password";
    }

    @PostMapping("/change-password")
    public String changePassword(
            @RequestParam String login,
            @RequestParam String oldPassword,
            @RequestParam String newPassword,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        try {
            authService.changePasswordByLogin(login, oldPassword, newPassword);
            auditService.logChangePassword(login, true, request.getRemoteAddr());
            // Письмо на почту пользователя со ссылкой на страницу смены пароля
            if (emailService != null) {
                authService.findEmailByLogin(login).ifPresent(email -> {
                    String changePasswordUrl = buildBaseUrl(request) + request.getContextPath() + "/web/change-password";
                    emailService.sendPasswordChangedNotification(email, changePasswordUrl);
                });
            }
            redirectAttributes.addAttribute("changed", "true");
            return "redirect:/web/login";
        } catch (Exception e) {
            auditService.logChangePassword(login, false, request.getRemoteAddr());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/change-password";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "web/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String forgotPassword(
            @RequestParam String email,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        Optional<String> tokenOpt = authService.requestPasswordReset(email != null ? email.trim() : "");
        if (tokenOpt.isPresent() && emailService != null) {
            String baseUrl = buildBaseUrl(request);
            String resetLink = baseUrl + request.getContextPath() + "/web/reset-password?token=" + tokenOpt.get();
            emailService.sendPasswordResetLink(email.trim(), resetLink);
        }
        redirectAttributes.addFlashAttribute("message", "Если указанный адрес зарегистрирован, на него отправлена ссылка для сброса пароля. Проверьте почту.");
        return "redirect:/web/login";
    }

    @GetMapping("/reset-password")
    public String resetPasswordForm(@RequestParam(required = false) String token, Model model) {
        if (token == null || token.isBlank()) {
            model.addAttribute("error", "Отсутствует ссылка для сброса. Запросите сброс пароля снова.");
            return "web/reset-password";
        }
        model.addAttribute("token", token);
        return "web/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes,
            HttpServletRequest request
    ) {
        if (newPassword == null || !newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Пароли не совпадают");
            return "redirect:/web/reset-password?token=" + (token != null ? token : "");
        }
        if (newPassword != null && newPassword.length() < 4) {
            redirectAttributes.addFlashAttribute("error", "Пароль должен быть не короче 4 символов");
            return "redirect:/web/reset-password?token=" + (token != null ? token : "");
        }
        try {
            java.util.Optional<String> userEmail = authService.resetPasswordByToken(token, newPassword);
            if (emailService != null) {
                userEmail.ifPresent(email -> {
                    String changePasswordUrl = buildBaseUrl(request) + request.getContextPath() + "/web/change-password";
                    emailService.sendPasswordChangedNotification(email, changePasswordUrl);
                });
            }
            redirectAttributes.addAttribute("changed", "true");
            return "redirect:/web/login";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/web/reset-password?token=" + (token != null ? token : "");
        }
    }

    private static String buildBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int port = request.getServerPort();
        boolean defaultPort = ("http".equals(scheme) && port == 80) || ("https".equals(scheme) && port == 443);
        if (defaultPort) return scheme + "://" + serverName;
        return scheme + "://" + serverName + ":" + port;
    }

    /** Общий дашборд (fallback); предпочтительно редирект на панель по роли. */
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("activeSection", "dashboard");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
            if ("STUDENT".equals(principal.getRole())) {
                studentRepository.findByUserId(principal.getId())
                        .ifPresent(s -> model.addAttribute("currentStudentId", s.getStudentId()));
            }
        }
        return "web/dashboard";
    }

    @GetMapping("/panel/admin")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public String panelAdmin(Model model) {
        addUserInfo(model);
        return "web/panel/admin";
    }

    @GetMapping("/panel/deanery")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String panelDeanery(Model model) {
        addUserInfo(model);
        return "web/panel/deanery";
    }

    @GetMapping("/panel/teacher")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public String panelTeacher(Model model) {
        addUserInfo(model);
        return "web/panel/teacher";
    }

    @GetMapping("/panel/student")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('STUDENT')")
    public String panelStudent(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
            studentRepository.findByUserId(principal.getId()).ifPresent(s -> {
                model.addAttribute("currentStudentId", s.getStudentId());
                model.addAttribute("studentFullName", s.getFullName());
                model.addAttribute("groupName", s.getGroupName());
                model.addAttribute("admissionYear", s.getAdmissionYear());
                model.addAttribute("grades", gradeService.getGradesByStudentId(s.getStudentId()));
                model.addAttribute("gradeSummary", gradeService.getStudentGradeSummary(s.getStudentId()));
                model.addAttribute("averageGrade", gradeService.calculateAverageGrade(s.getStudentId()));
            });
        }
        return "web/panel/student";
    }

    private void addUserInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
    }
}
