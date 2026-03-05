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
import ru.ryabov.studentperformance.entity.StudyGroup;
import ru.ryabov.studentperformance.repository.FacultyRepository;
import ru.ryabov.studentperformance.repository.GroupRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AuditService;

/**
 * Веб: список групп и CRUD (формы + POST).
 */
@Controller
@RequestMapping("/web/groups")
@PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
public class WebGroupsController {

    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private FacultyRepository facultyRepository;
    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping
    public String list(@RequestParam(required = false) String search, Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Группы");
        model.addAttribute("activeSection", "groups");
        model.addAttribute("activeTab", "groups");
        var all = groupRepository.findAllWithFaculty();
        var list = (search == null || search.isBlank())
                ? all
                : all.stream()
                        .filter(g -> (g.getName() != null && g.getName().toLowerCase().contains(search.toLowerCase()))
                                || (g.getSpecialization() != null && g.getSpecialization().toLowerCase().contains(search.toLowerCase()))
                                || (g.getFaculty() != null && g.getFaculty().getName() != null && g.getFaculty().getName().toLowerCase().contains(search.toLowerCase())))
                        .toList();
        model.addAttribute("groups", list);
        model.addAttribute("search", search != null ? search : "");
        return "web/groups/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String createForm(Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Новая группа");
        model.addAttribute("activeSection", "groups");
        model.addAttribute("activeTab", "groups");
        model.addAttribute("faculties", facultyRepository.findAllByOrderByName());
        model.addAttribute("group", new StudyGroup());
        return "web/groups/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String create(@RequestParam String name, @RequestParam(required = false) Long facultyId,
                        @RequestParam Integer admissionYear, @RequestParam(required = false) String specialization,
                        RedirectAttributes ra) {
        Faculty faculty = facultyId != null ? facultyRepository.findById(facultyId).orElse(null) : null;
        StudyGroup g = new StudyGroup(name, faculty, admissionYear, specialization);
        groupRepository.save(g);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "StudyGroup", g.getGroupId(), "name=" + name);
        ra.addFlashAttribute("message", "Группа создана");
        return "redirect:/web/groups";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        addUserInfo(model);
        var opt = groupRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Группа не найдена");
            return "redirect:/web/groups";
        }
        model.addAttribute("pageTitle", "Редактировать группу");
        model.addAttribute("activeSection", "groups");
        model.addAttribute("activeTab", "groups");
        model.addAttribute("faculties", facultyRepository.findAllByOrderByName());
        model.addAttribute("group", opt.get());
        return "web/groups/form";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String edit(@PathVariable Long id, @RequestParam String name,
                      @RequestParam(required = false) Long facultyId, @RequestParam Integer admissionYear,
                      @RequestParam(required = false) String specialization, RedirectAttributes ra) {
        var opt = groupRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Группа не найдена");
            return "redirect:/web/groups";
        }
        StudyGroup g = opt.get();
        g.setName(name);
        g.setAdmissionYear(admissionYear);
        g.setSpecialization(specialization);
        g.setFaculty(facultyId != null ? facultyRepository.findById(facultyId).orElse(null) : null);
        groupRepository.save(g);
        if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "StudyGroup", id, null);
        ra.addFlashAttribute("message", "Группа обновлена");
        return "redirect:/web/groups";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        if (groupRepository.existsById(id)) {
            groupRepository.deleteById(id);
            if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "StudyGroup", id, null);
            ra.addFlashAttribute("message", "Группа удалена");
        } else {
            ra.addFlashAttribute("error", "Группа не найдена");
        }
        return "redirect:/web/groups";
    }

    private void addUserInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
    }
}
