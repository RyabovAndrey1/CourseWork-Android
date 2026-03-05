package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.StudentScheduleService;

/**
 * Веб: страница «Расписание» для студента — даты занятий, дисциплины, присутствие, баллы.
 */
@Controller
@RequestMapping("/web/student")
@PreAuthorize("hasRole('STUDENT')")
public class WebStudentScheduleController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentScheduleService studentScheduleService;

    @GetMapping("/schedule")
    public String schedule(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
        model.addAttribute("pageTitle", "Расписание");
        model.addAttribute("activeSection", "schedule");

        var studentOpt = auth != null && auth.getPrincipal() instanceof UserPrincipal p
                ? studentRepository.findByUserId(p.getId())
                : java.util.Optional.<ru.ryabov.studentperformance.entity.Student>empty();

        if (studentOpt.isEmpty()) {
            model.addAttribute("scheduleItems", java.util.List.<ru.ryabov.studentperformance.dto.schedule.ScheduleItemDto>of());
            return "web/student/schedule";
        }
        Long studentId = studentOpt.get().getStudentId();
        model.addAttribute("scheduleItems", studentScheduleService.getScheduleForStudent(studentId));
        model.addAttribute("studentFullName", studentOpt.get().getFullName());
        return "web/student/schedule";
    }
}
