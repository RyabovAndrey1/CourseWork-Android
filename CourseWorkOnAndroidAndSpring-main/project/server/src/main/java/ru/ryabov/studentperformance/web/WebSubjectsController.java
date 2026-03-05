package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.entity.Subject;
import ru.ryabov.studentperformance.repository.SubjectRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AuditService;

@Controller
@RequestMapping("/web/subjects")
@PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
public class WebSubjectsController {

    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping
    public String list(@RequestParam(required = false) String search, Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Дисциплины");
        model.addAttribute("activeSection", "subjects");
        model.addAttribute("activeTab", "subjects");
        var all = subjectRepository.findAllOrderByName();
        var list = (search == null || search.isBlank())
                ? all
                : all.stream()
                        .filter(s -> (s.getName() != null && s.getName().toLowerCase().contains(search.toLowerCase()))
                                || (s.getCode() != null && s.getCode().toLowerCase().contains(search.toLowerCase())))
                        .toList();
        model.addAttribute("subjects", list);
        model.addAttribute("search", search != null ? search : "");
        return "web/subjects/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String createForm(Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Новая дисциплина");
        model.addAttribute("activeSection", "subjects");
        model.addAttribute("activeTab", "subjects");
        model.addAttribute("subject", new Subject());
        return "web/subjects/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String create(@RequestParam String name, @RequestParam(required = false) String code,
                        @RequestParam(required = false) java.math.BigDecimal credits,
                        @RequestParam(required = false) Integer totalHours,
                        @RequestParam(required = false) Integer lectureHours, @RequestParam(required = false) Integer practiceHours,
                        @RequestParam(required = false) Integer labHours, @RequestParam(required = false) String controlType,
                        @RequestParam(required = false) String description, RedirectAttributes ra) {
        Subject.ControlType ct = null;
        if (controlType != null && !controlType.isBlank()) {
            try { ct = Subject.ControlType.valueOf(controlType); } catch (Exception ignored) {}
        }
        Subject s = new Subject(name, code, credits != null ? credits : java.math.BigDecimal.ZERO,
                totalHours, lectureHours, practiceHours, labHours, ct, description);
        subjectRepository.save(s);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Subject", s.getSubjectId(), "name=" + name);
        ra.addFlashAttribute("message", "Дисциплина создана");
        return "redirect:/web/subjects";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        addUserInfo(model);
        var opt = subjectRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Дисциплина не найдена");
            return "redirect:/web/subjects";
        }
        model.addAttribute("pageTitle", "Редактировать дисциплину");
        model.addAttribute("activeSection", "subjects");
        model.addAttribute("activeTab", "subjects");
        model.addAttribute("subject", opt.get());
        return "web/subjects/form";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String edit(@PathVariable Long id, @RequestParam String name, @RequestParam(required = false) String code,
                      @RequestParam(required = false) java.math.BigDecimal credits,
                      @RequestParam(required = false) Integer totalHours,
                      @RequestParam(required = false) Integer lectureHours, @RequestParam(required = false) Integer practiceHours,
                      @RequestParam(required = false) Integer labHours, @RequestParam(required = false) String controlType,
                      @RequestParam(required = false) String description, RedirectAttributes ra) {
        var opt = subjectRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Дисциплина не найдена");
            return "redirect:/web/subjects";
        }
        Subject s = opt.get();
        s.setName(name);
        s.setCode(code);
        s.setCredits(credits);
        s.setTotalHours(totalHours);
        s.setLectureHours(lectureHours);
        s.setPracticeHours(practiceHours);
        s.setLabHours(labHours);
        if (controlType != null && !controlType.isBlank()) {
            try { s.setControlType(Subject.ControlType.valueOf(controlType)); } catch (Exception ignored) {}
        }
        s.setDescription(description);
        subjectRepository.save(s);
        if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Subject", id, null);
        ra.addFlashAttribute("message", "Дисциплина обновлена");
        return "redirect:/web/subjects";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        if (subjectRepository.existsById(id)) {
            subjectRepository.deleteById(id);
            if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Subject", id, null);
            ra.addFlashAttribute("message", "Дисциплина удалена");
        } else {
            ra.addFlashAttribute("error", "Дисциплина не найдена");
        }
        return "redirect:/web/subjects";
    }

    private void addUserInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
    }
}
