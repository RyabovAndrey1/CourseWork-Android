package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.EmailService;
import org.springframework.security.core.Authentication;

/**
 * Диагностика почты: статус настройки и отправка тестового письма (для админа).
 */
@Controller
@RequestMapping("/web/admin")
public class WebMailTestController {

    @Autowired(required = false)
    private EmailService emailService;

    @GetMapping("/test-email")
    @PreAuthorize("hasRole('ADMIN')")
    public String page(Model model, Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
        model.addAttribute("pageTitle", "Проверка почты");
        model.addAttribute("activeSection", "test-email");
        model.addAttribute("activeTab", "test-email");
        model.addAttribute("mailConfigured", emailService != null && emailService.isMailConfigured());
        return "web/admin/test-email";
    }

    @PostMapping("/test-email")
    @PreAuthorize("hasRole('ADMIN')")
    public String sendTest(
            @RequestParam(required = false) String email,
            RedirectAttributes ra) {
        if (emailService == null) {
            ra.addFlashAttribute("error", "Сервис почты недоступен.");
            return "redirect:/web/admin/test-email";
        }
        String err = emailService.sendTestEmail(email);
        if (err == null) {
            ra.addFlashAttribute("message", "Тестовое письмо отправлено на " + email + ". Проверьте папку «Входящие» и «Спам».");
        } else {
            ra.addFlashAttribute("error", err);
        }
        return "redirect:/web/admin/test-email";
    }
}
