package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.entity.Faculty;
import ru.ryabov.studentperformance.repository.FacultyRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AuditService;

@Controller
@RequestMapping("/web/faculties")
@PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
public class WebFacultiesController {

    @Autowired
    private FacultyRepository facultyRepository;
    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping
    public String list(@RequestParam(required = false) String search, Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Факультеты");
        model.addAttribute("activeSection", "faculties");
        model.addAttribute("activeTab", "faculties");
        var all = facultyRepository.findAllByOrderByName();
        var list = (search == null || search.isBlank())
                ? all
                : all.stream()
                        .filter(f -> (f.getName() != null && f.getName().toLowerCase().contains(search.toLowerCase()))
                                || (f.getDeanName() != null && f.getDeanName().toLowerCase().contains(search.toLowerCase())))
                        .toList();
        model.addAttribute("faculties", list);
        model.addAttribute("search", search != null ? search : "");
        return "web/faculties/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String createForm(Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Новый факультет");
        model.addAttribute("activeSection", "faculties");
        model.addAttribute("activeTab", "faculties");
        model.addAttribute("faculty", new Faculty());
        return "web/faculties/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String create(@RequestParam String name, @RequestParam(required = false) String deanName, RedirectAttributes ra) {
        Faculty f = new Faculty(name, deanName);
        facultyRepository.save(f);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Faculty", f.getFacultyId(), "name=" + name);
        ra.addFlashAttribute("message", "Факультет создан");
        return "redirect:/web/faculties";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        addUserInfo(model);
        var opt = facultyRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Факультет не найден");
            return "redirect:/web/faculties";
        }
        model.addAttribute("pageTitle", "Редактировать факультет");
        model.addAttribute("activeSection", "faculties");
        model.addAttribute("activeTab", "faculties");
        model.addAttribute("faculty", opt.get());
        return "web/faculties/form";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String edit(@PathVariable Long id, @RequestParam String name, @RequestParam(required = false) String deanName, RedirectAttributes ra) {
        var opt = facultyRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Факультет не найден");
            return "redirect:/web/faculties";
        }
        Faculty f = opt.get();
        f.setName(name);
        f.setDeanName(deanName);
        facultyRepository.save(f);
        if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Faculty", id, null);
        ra.addFlashAttribute("message", "Факультет обновлён");
        return "redirect:/web/faculties";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        if (facultyRepository.existsById(id)) {
            facultyRepository.deleteById(id);
            if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Faculty", id, null);
            ra.addFlashAttribute("message", "Факультет удалён");
        } else {
            ra.addFlashAttribute("error", "Факультет не найден");
        }
        return "redirect:/web/faculties";
    }

    private void addUserInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
    }
}
