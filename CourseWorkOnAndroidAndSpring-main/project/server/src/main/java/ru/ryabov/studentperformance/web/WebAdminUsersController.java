package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.UserRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AuditService;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/web/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class WebAdminUsersController {

    private static final String DEFAULT_PASSWORD = "password123";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired(required = false)
    private AuditService auditService;

    private void addUserInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Новый пользователь");
        model.addAttribute("activeSection", "users");
        model.addAttribute("activeTab", "users");
        model.addAttribute("roles", User.Role.values());
        return "web/admin/user-form";
    }

    @PostMapping("/create")
    public String create(@RequestParam String login, @RequestParam String email,
                        @RequestParam String lastName, @RequestParam String firstName, @RequestParam(required = false) String middleName,
                        @RequestParam String role, RedirectAttributes ra) {
        if (userRepository.findByLogin(login).isPresent()) {
            ra.addFlashAttribute("error", "Пользователь с таким логином уже существует");
            return "redirect:/web/admin/users/create";
        }
        if (userRepository.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "Пользователь с таким email уже существует");
            return "redirect:/web/admin/users/create";
        }
        User.Role r;
        try {
            r = User.Role.valueOf(role);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Недопустимая роль");
            return "redirect:/web/admin/users/create";
        }
        User user = new User(login, passwordEncoder.encode(DEFAULT_PASSWORD), email, lastName, firstName, middleName, r);
        userRepository.save(user);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "User", user.getUserId(), "login=" + login);
        ra.addFlashAttribute("message", "Пользователь создан. Пароль по умолчанию: " + DEFAULT_PASSWORD);
        return "redirect:/web/admin/users";
    }

    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        addUserInfo(model);
        var opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/web/admin/users";
        }
        model.addAttribute("pageTitle", "Редактировать пользователя");
        model.addAttribute("activeSection", "users");
        model.addAttribute("activeTab", "users");
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("user", opt.get());
        return "web/admin/user-form";
    }

    @PostMapping("/edit/{id}")
    public String edit(@PathVariable Long id, @RequestParam String email,
                      @RequestParam String lastName, @RequestParam String firstName, @RequestParam(required = false) String middleName,
                      @RequestParam String role, @RequestParam(value = "active", defaultValue = "false") boolean active, RedirectAttributes ra) {
        var opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/web/admin/users";
        }
        User u = opt.get();
        u.setEmail(email);
        u.setLastName(lastName);
        u.setFirstName(firstName);
        u.setMiddleName(middleName);
        u.setIsActive(active);
        try {
            u.setRole(User.Role.valueOf(role));
        } catch (Exception ignored) {}
        userRepository.save(u);
        if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "User", id, "login=" + u.getLogin());
        ra.addFlashAttribute("message", "Пользователь обновлён");
        return "redirect:/web/admin/users";
    }

    @PostMapping("/deactivate/{id}")
    public String deactivate(@PathVariable Long id, RedirectAttributes ra) {
        var opt = userRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/web/admin/users";
        }
        User u = opt.get();
        u.setIsActive(false);
        userRepository.save(u);
        if (auditService != null) auditService.logAction("DEACTIVATE", SecurityUtils.getCurrentActor(), "User", id, "login=" + u.getLogin());
        ra.addFlashAttribute("message", "Пользователь деактивирован");
        return "redirect:/web/admin/users";
    }
}
