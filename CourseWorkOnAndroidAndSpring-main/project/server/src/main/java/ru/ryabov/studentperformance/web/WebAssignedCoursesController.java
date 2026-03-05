package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.entity.AssignedCourse;
import ru.ryabov.studentperformance.entity.StudyGroup;
import ru.ryabov.studentperformance.entity.Subject;
import ru.ryabov.studentperformance.entity.Teacher;
import ru.ryabov.studentperformance.repository.AssignedCourseRepository;
import ru.ryabov.studentperformance.repository.GroupRepository;
import ru.ryabov.studentperformance.repository.SubjectRepository;
import ru.ryabov.studentperformance.repository.TeacherRepository;
import ru.ryabov.studentperformance.security.UserPrincipal;

import org.springframework.security.core.Authentication;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Веб: назначение преподавателя на дисциплину (преподаватель — группа — дисциплина) для админа и деканата.
 */
@Controller
@RequestMapping("/web/admin")
public class WebAssignedCoursesController {

    @Autowired
    private AssignedCourseRepository assignedCourseRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private SubjectRepository subjectRepository;

    @GetMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional(readOnly = true)
    public String list(Model model, Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
        model.addAttribute("pageTitle", "Назначение преподавателя на дисциплину");
        model.addAttribute("activeSection", "assignments");
        model.addAttribute("activeTab", "assignments");
        List<AssignedCourse> list = assignedCourseRepository.findAllWithTeacherGroupSubject();
        model.addAttribute("assignments", list.stream().map(this::toRow).collect(Collectors.toList()));
        List<Teacher> teachers = teacherRepository.findAllWithUser().stream()
                .sorted(Comparator.comparing(t -> t.getFullName() != null ? t.getFullName() : ""))
                .toList();
        List<StudyGroup> groups = groupRepository.findAllWithFaculty().stream()
                .sorted(Comparator.comparing(StudyGroup::getName))
                .toList();
        List<Subject> subjects = subjectRepository.findAllOrderByName();
        model.addAttribute("teachers", teachers);
        model.addAttribute("groups", groups);
        model.addAttribute("subjects", subjects);
        int currentYear = java.time.Year.now().getValue();
        model.addAttribute("currentYear", currentYear);
        return "web/admin/assignments";
    }

    @PostMapping("/assignments/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional
    public String create(
            @RequestParam Long teacherId,
            @RequestParam Long groupId,
            @RequestParam Long subjectId,
            @RequestParam int academicYear,
            @RequestParam int semester,
            RedirectAttributes ra) {
        if (assignedCourseRepository.existsByTeacherAndGroupAndSubjectAndYearAndSemester(teacherId, groupId, subjectId, academicYear, semester)) {
            ra.addFlashAttribute("error", "Такое назначение (преподаватель — группа — дисциплина — год — семестр) уже существует.");
            return "redirect:/web/admin/assignments";
        }
        Teacher teacher = teacherRepository.findById(teacherId).orElse(null);
        StudyGroup group = groupRepository.findById(groupId).orElse(null);
        Subject subject = subjectRepository.findById(subjectId).orElse(null);
        if (teacher == null || group == null || subject == null) {
            ra.addFlashAttribute("error", "Выберите преподавателя, группу и дисциплину.");
            return "redirect:/web/admin/assignments";
        }
        if (semester < 1 || semester > 12 || academicYear < 2000 || academicYear > 2100) {
            ra.addFlashAttribute("error", "Учебный год 2000–2100, семестр 1–12.");
            return "redirect:/web/admin/assignments";
        }
        AssignedCourse ac = new AssignedCourse(teacher, group, subject, academicYear, semester);
        ac.setCreatedAt(LocalDateTime.now());
        assignedCourseRepository.save(ac);
        ra.addFlashAttribute("message", "Назначение добавлено.");
        return "redirect:/web/admin/assignments";
    }

    @PostMapping("/assignments/delete/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY')")
    @Transactional
    public String delete(@PathVariable Long id, RedirectAttributes ra) {
        assignedCourseRepository.findById(id).ifPresent(assignedCourseRepository::delete);
        ra.addFlashAttribute("message", "Назначение удалено.");
        return "redirect:/web/admin/assignments";
    }

    private AssignmentRow toRow(AssignedCourse ac) {
        String teacherName = ac.getTeacher() != null ? ac.getTeacher().getFullName() : "—";
        String groupName = ac.getGroup() != null ? ac.getGroup().getName() : "—";
        String subjectName = ac.getSubject() != null ? ac.getSubject().getName() : "—";
        return new AssignmentRow(
                ac.getAssignmentId(),
                teacherName,
                groupName,
                subjectName,
                ac.getAcademicYear(),
                ac.getSemester()
        );
    }

    public record AssignmentRow(Long id, String teacherName, String groupName, String subjectName, Integer academicYear, Integer semester) {}
}
