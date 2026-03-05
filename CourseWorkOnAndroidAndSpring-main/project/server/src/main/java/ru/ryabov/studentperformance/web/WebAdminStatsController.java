package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.dto.user.TeacherDto;
import ru.ryabov.studentperformance.dto.user.UserDto;
import ru.ryabov.studentperformance.entity.Department;
import ru.ryabov.studentperformance.entity.Teacher;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.*;
import ru.ryabov.studentperformance.security.SecurityUtils;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AuditService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Веб: страницы админки — статистика и пользователи (только админ); преподаватели (админ и деканат).
 */
@Controller
@RequestMapping("/web/admin")
public class WebAdminStatsController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private FacultyRepository facultyRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired(required = false)
    private AuditService auditService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public String stats(Model model, org.springframework.security.core.Authentication auth) {
        addUserAndRole(model, auth);
        model.addAttribute("pageTitle", "Статистика системы");
        model.addAttribute("activeSection", "stats");
        model.addAttribute("activeTab", "stats");
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalStudents", studentRepository.count());
        model.addAttribute("totalTeachers", teacherRepository.count());
        model.addAttribute("totalGroups", groupRepository.count());
        model.addAttribute("totalFaculties", facultyRepository.count());
        model.addAttribute("totalDepartments", departmentRepository.count());
        return "web/admin/stats";
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String users(Model model, org.springframework.security.core.Authentication auth) {
        addUserAndRole(model, auth);
        model.addAttribute("pageTitle", "Пользователи");
        model.addAttribute("activeSection", "users");
        model.addAttribute("activeTab", "users");
        var list = userRepository.findAll().stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
        model.addAttribute("users", list);
        return "web/admin/users";
    }

    @GetMapping("/teachers")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional(readOnly = true)
    public String teachers(Model model, org.springframework.security.core.Authentication auth) {
        addUserAndRole(model, auth);
        model.addAttribute("pageTitle", "Преподаватели");
        model.addAttribute("activeSection", "teachers");
        model.addAttribute("activeTab", "teachers");
        var list = teacherRepository.findAllWithUser().stream()
                .map(this::toTeacherDto)
                .collect(Collectors.toList());
        model.addAttribute("teachers", list);
        return "web/admin/teachers";
    }

    @GetMapping("/teachers/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional(readOnly = true)
    public String teacherCreateForm(Model model, org.springframework.security.core.Authentication auth) {
        addUserAndRole(model, auth);
        model.addAttribute("pageTitle", "Новый преподаватель");
        model.addAttribute("activeSection", "teachers");
        model.addAttribute("activeTab", "teachers");
        List<User> teacherRoleUsers = userRepository.findActiveUsersByRole(User.Role.TEACHER);
        List<User> withoutTeacher = teacherRoleUsers.stream()
                .filter(u -> teacherRepository.findByUserId(u.getUserId()).isEmpty())
                .toList();
        model.addAttribute("usersWithoutTeacher", withoutTeacher);
        model.addAttribute("departments", departmentRepository.findAll());
        return "web/admin/teacher-form";
    }

    @PostMapping("/teachers/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional
    public String teacherCreate(@RequestParam Long userId,
                               @RequestParam(required = false) Long departmentId,
                               @RequestParam(required = false) String academicDegree,
                               @RequestParam(required = false) String position,
                               RedirectAttributes ra) {
        if (teacherRepository.findByUserId(userId).isPresent()) {
            ra.addFlashAttribute("error", "У этого пользователя уже есть запись преподавателя");
            return "redirect:/web/admin/teachers/create";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            ra.addFlashAttribute("error", "Пользователь не найден");
            return "redirect:/web/admin/teachers/create";
        }
        Department department = departmentId != null ? departmentRepository.findById(departmentId).orElse(null) : null;
        Teacher teacher = new Teacher(user, department, academicDegree, position);
        teacherRepository.save(teacher);
        if (auditService != null) auditService.logAction("CREATE", SecurityUtils.getCurrentActor(), "Teacher", teacher.getTeacherId(), "userId=" + userId);
        ra.addFlashAttribute("message", "Преподаватель добавлен");
        return "redirect:/web/admin/teachers";
    }

    @GetMapping("/teachers/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional(readOnly = true)
    public String teacherEditForm(@PathVariable Long id, Model model, RedirectAttributes ra,
                                 org.springframework.security.core.Authentication auth) {
        addUserAndRole(model, auth);
        var opt = teacherRepository.findByIdWithUserAndDepartment(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Преподаватель не найден");
            return "redirect:/web/admin/teachers";
        }
        model.addAttribute("pageTitle", "Редактировать преподавателя");
        model.addAttribute("activeSection", "teachers");
        model.addAttribute("activeTab", "teachers");
        model.addAttribute("teacher", opt.get());
        model.addAttribute("departments", departmentRepository.findAll());
        return "web/admin/teacher-form";
    }

    @PostMapping("/teachers/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional
    public String teacherEdit(@PathVariable Long id,
                             @RequestParam(required = false) Long departmentId,
                             @RequestParam(required = false) String academicDegree,
                             @RequestParam(required = false) String position,
                             RedirectAttributes ra) {
        var opt = teacherRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Преподаватель не найден");
            return "redirect:/web/admin/teachers";
        }
        Teacher t = opt.get();
        t.setDepartment(departmentId != null ? departmentRepository.findById(departmentId).orElse(null) : null);
        t.setAcademicDegree(academicDegree);
        t.setPosition(position);
        teacherRepository.save(t);
        if (auditService != null) auditService.logAction("UPDATE", SecurityUtils.getCurrentActor(), "Teacher", id, null);
        ra.addFlashAttribute("message", "Преподаватель обновлён");
        return "redirect:/web/admin/teachers";
    }

    @PostMapping("/teachers/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional
    public String teacherDelete(@PathVariable Long id, RedirectAttributes ra) {
        var opt = teacherRepository.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Преподаватель не найден");
            return "redirect:/web/admin/teachers";
        }
        teacherRepository.delete(opt.get());
        if (auditService != null) auditService.logAction("DELETE", SecurityUtils.getCurrentActor(), "Teacher", id, null);
        ra.addFlashAttribute("message", "Запись преподавателя удалена (пользователь сохранён)");
        return "redirect:/web/admin/teachers";
    }

    private void addUserAndRole(Model model, org.springframework.security.core.Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
    }

    private UserDto toUserDto(User u) {
        return new UserDto(
                u.getUserId(), u.getLogin(), u.getEmail(),
                u.getLastName(), u.getFirstName(), u.getMiddleName(),
                u.getRole().name(), u.getIsActive()
        );
    }

    private TeacherDto toTeacherDto(Teacher t) {
        User u = t.getUser();
        Department d = t.getDepartment();
        return new TeacherDto(
                t.getTeacherId(), u != null ? u.getUserId() : null, t.getFullName(),
                u != null ? u.getLogin() : null, u != null ? u.getEmail() : null,
                t.getAcademicDegree(), t.getPosition(),
                d != null ? d.getName() : null,
                d != null && d.getFaculty() != null ? d.getFaculty().getName() : null,
                d != null ? d.getDepartmentId() : null
        );
    }
}
