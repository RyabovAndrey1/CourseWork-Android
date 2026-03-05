package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.entity.Department;
import ru.ryabov.studentperformance.entity.Faculty;
import ru.ryabov.studentperformance.repository.DepartmentRepository;
import ru.ryabov.studentperformance.repository.FacultyRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AuditService;

import java.util.List;

/**
 * Веб-CRUD: кафедры (админ и деканат).
 */
@Controller
@RequestMapping("/web/departments")
@PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
public class WebDepartmentsController {

    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private FacultyRepository facultyRepository;
    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping
    @Transactional(readOnly = true)
    public String list(@RequestParam(required = false) String search,
                       @RequestParam(required = false) Long facultyId,
                       Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Кафедры");
        model.addAttribute("activeSection", "departments");
        model.addAttribute("activeTab", "departments");
        model.addAttribute("faculties", facultyRepository.findAllByOrderByName());
        List<Department> all = departmentRepository.findAllByOrderByName();
        if (facultyId != null) {
            all = all.stream().filter(d -> d.getFaculty() != null && facultyId.equals(d.getFaculty().getFacultyId())).toList();
        }
        if (search != null && !search.isBlank()) {
            String term = search.trim().toLowerCase();
            all = all.stream()
                    .filter(d -> (d.getName() != null && d.getName().toLowerCase().contains(term))
                            || (d.getHeadName() != null && d.getHeadName().toLowerCase().contains(term))
                            || (d.getFaculty() != null && d.getFaculty().getName() != null && d.getFaculty().getName().toLowerCase().contains(term)))
                    .toList();
        }
        model.addAttribute("departments", all);
        model.addAttribute("search", search != null ? search : "");
        model.addAttribute("facultyId", facultyId);
        return "web/departments/list";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Новая кафедра");
        model.addAttribute("activeSection", "departments");
        model.addAttribute("activeTab", "departments");
        model.addAttribute("department", new Department());
        model.addAttribute("faculties", facultyRepository.findAllByOrderByName());
        return "web/departments/form";
    }

    @PostMapping("/create")
    @Transactional
    public String create(@RequestParam String name,
                         @RequestParam(required = false) Long facultyId,
                         @RequestParam(required = false) String headName,
                         RedirectAttributes ra) {
        Faculty faculty = facultyId != null ? facultyRepository.findById(facultyId).orElse(null) : null;
        Department d = new Department(name, faculty, headName);
        departmentRepository.save(d);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Department", d.getDepartmentId(), "name=" + name);
        ra.addFlashAttribute("message", "Кафедра создана");
        return "redirect:/web/departments";
    }

    @GetMapping("/edit/{id}")
    @Transactional(readOnly = true)
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        addUserInfo(model);
        var opt = departmentRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Кафедра не найдена");
            return "redirect:/web/departments";
        }
        model.addAttribute("pageTitle", "Редактировать кафедру");
        model.addAttribute("activeSection", "departments");
        model.addAttribute("activeTab", "departments");
        model.addAttribute("department", opt.get());
        model.addAttribute("faculties", facultyRepository.findAllByOrderByName());
        return "web/departments/form";
    }

    @PostMapping("/edit/{id}")
    @Transactional
    public String edit(@PathVariable Long id,
                       @RequestParam String name,
                       @RequestParam(required = false) Long facultyId,
                       @RequestParam(required = false) String headName,
                       RedirectAttributes ra) {
        var opt = departmentRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Кафедра не найдена");
            return "redirect:/web/departments";
        }
        Department d = opt.get();
        d.setName(name);
        d.setFaculty(facultyId != null ? facultyRepository.findById(facultyId).orElse(null) : null);
        d.setHeadName(headName);
        departmentRepository.save(d);
        if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Department", id, null);
        ra.addFlashAttribute("message", "Кафедра обновлена");
        return "redirect:/web/departments";
    }

    @PostMapping("/delete/{id}")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        var opt = departmentRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Кафедра не найдена");
            return "redirect:/web/departments";
        }
        departmentRepository.delete(opt.get());
        if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Department", id, null);
        ra.addFlashAttribute("message", "Кафедра удалена");
        return "redirect:/web/departments";
    }

    private void addUserInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
    }
}
