package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.dto.user.StudentDto;
import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.entity.StudyGroup;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.GroupRepository;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.repository.UserRepository;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AuditService;

import java.util.stream.Collectors;

/**
 * Веб-CRUD: студенты (только администратор и деканат; преподаватель не имеет доступа к списку).
 */
@Controller
@RequestMapping("/web/students")
@PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
public class WebStudentsController {

    private static final String DEFAULT_PASSWORD = "password123";

    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping
    @Transactional(readOnly = true)
    public String list(@RequestParam(required = false) String search, Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Студенты");
        model.addAttribute("activeSection", "students");
        model.addAttribute("activeTab", "students");
        var all = studentRepository.findAll();
        var list = (search == null || search.isBlank())
                ? all
                : all.stream()
                        .filter(s -> {
                            String fn = s.getFullName();
                            String log = s.getUser() != null ? s.getUser().getLogin() : "";
                            String term = search.toLowerCase();
                            return (fn != null && fn.toLowerCase().contains(term)) || log.toLowerCase().contains(term);
                        })
                        .toList();
        var result = list.stream()
                .map(s -> new StudentDto(
                        s.getStudentId(),
                        s.getUser() != null ? s.getUser().getUserId() : null,
                        s.getFullName(),
                        s.getUser() != null ? s.getUser().getLogin() : null,
                        s.getUser() != null ? s.getUser().getEmail() : null,
                        s.getRecordBookNumber(),
                        s.getGroup() != null ? s.getGroup().getName() : null,
                        s.getGroup() != null ? s.getGroup().getGroupId() : null,
                        s.getGroup() != null && s.getGroup().getFaculty() != null
                                ? s.getGroup().getFaculty().getName() : null,
                        s.getAdmissionYear()
                ))
                .collect(Collectors.toList());
        model.addAttribute("students", result);
        model.addAttribute("search", search != null ? search : "");
        return "web/students/list";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    public String createForm(Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Новый студент");
        model.addAttribute("activeSection", "students");
        model.addAttribute("activeTab", "students");
        model.addAttribute("groups", groupRepository.findAllWithFaculty());
        return "web/students/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional
    public String create(@RequestParam String login, @RequestParam String email,
                         @RequestParam String lastName, @RequestParam String firstName, @RequestParam(required = false) String middleName,
                         @RequestParam(required = false) Long groupId, @RequestParam(required = false) String recordBookNumber,
                         @RequestParam(required = false) Integer admissionYear,
                         RedirectAttributes ra) {
        if (userRepository.findByLogin(login).isPresent()) {
            ra.addFlashAttribute("error", "Пользователь с таким логином уже существует");
            return "redirect:/web/students/create";
        }
        User user = new User(login, passwordEncoder.encode(DEFAULT_PASSWORD), email, lastName, firstName, middleName, User.Role.STUDENT);
        user = userRepository.save(user);
        StudyGroup group = groupId != null ? groupRepository.findById(groupId).orElse(null) : null;
        Student student = new Student(user, group, recordBookNumber, admissionYear, null, null, null);
        studentRepository.save(student);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Student", student.getStudentId(), "login=" + login);
        ra.addFlashAttribute("message", "Студент создан. Пароль по умолчанию: " + DEFAULT_PASSWORD);
        return "redirect:/web/students";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional(readOnly = true)
    public String editForm(@PathVariable Long id, Model model, RedirectAttributes ra) {
        addUserInfo(model);
        var opt = studentRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Студент не найден");
            return "redirect:/web/students";
        }
        model.addAttribute("pageTitle", "Редактировать студента");
        model.addAttribute("activeSection", "students");
        model.addAttribute("activeTab", "students");
        model.addAttribute("groups", groupRepository.findAllWithFaculty());
        model.addAttribute("student", opt.get());
        return "web/students/form";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional
    public String edit(@PathVariable Long id, @RequestParam String email,
                      @RequestParam String lastName, @RequestParam String firstName, @RequestParam(required = false) String middleName,
                      @RequestParam(required = false) Long groupId, @RequestParam(required = false) String recordBookNumber,
                      @RequestParam(required = false) Integer admissionYear, RedirectAttributes ra) {
        var opt = studentRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Студент не найден");
            return "redirect:/web/students";
        }
        Student s = opt.get();
        User u = s.getUser();
        if (u != null) {
            u.setEmail(email);
            u.setLastName(lastName);
            u.setFirstName(firstName);
            u.setMiddleName(middleName);
            userRepository.save(u);
        }
        s.setGroup(groupId != null ? groupRepository.findById(groupId).orElse(null) : null);
        s.setRecordBookNumber(recordBookNumber);
        s.setAdmissionYear(admissionYear);
        studentRepository.save(s);
        if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Student", id, null);
        ra.addFlashAttribute("message", "Студент обновлён");
        return "redirect:/web/students";
    }

    @PostMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        var opt = studentRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Студент не найден");
            return "redirect:/web/students";
        }
        Student s = opt.get();
        User u = s.getUser();
        studentRepository.delete(s);
        if (u != null) userRepository.delete(u);
        if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Student", id, null);
        ra.addFlashAttribute("message", "Студент удалён");
        return "redirect:/web/students";
    }

    private void addUserInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
    }
}
