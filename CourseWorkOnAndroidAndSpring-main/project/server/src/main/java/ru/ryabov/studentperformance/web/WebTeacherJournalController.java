package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.dto.grade.CreateGradeRequest;
import ru.ryabov.studentperformance.dto.journal.LessonRecordDto;
import ru.ryabov.studentperformance.entity.*;
import ru.ryabov.studentperformance.repository.*;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.AttendanceService;
import ru.ryabov.studentperformance.service.GradeService;
import ru.ryabov.studentperformance.service.TeacherJournalService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Журнал преподавателя: выставление оценок и посещаемости (контроль успеваемости).
 */
@Controller
@RequestMapping("/web/teacher/journal")
@PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
public class WebTeacherJournalController {

    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private GradeTypeRepository gradeTypeRepository;
    @Autowired
    private GradeService gradeService;
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private AssignedCourseRepository assignedCourseRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private TeacherJournalService teacherJournalService;

    private static final int JOURNAL_PAGE_SIZE = 10;

    @GetMapping
    @Transactional(readOnly = true)
    public String journal(@RequestParam(required = false) Long groupId,
                          @RequestParam(required = false) Long subjectId,
                          @RequestParam(required = false) Long gradeTypeId,
                          @RequestParam(required = false) String dateFrom,
                          @RequestParam(required = false) String dateTo,
                          @RequestParam(defaultValue = "0") int page,
                          Model model) {
        addUserInfo(model);
        model.addAttribute("pageTitle", "Журнал");
        model.addAttribute("activeSection", "journal");
        model.addAttribute("activeTab", "journal");
        model.addAttribute("groups", getGroupsForCurrentUser());
        model.addAttribute("subjects", getSubjectsForCurrentUser());
        model.addAttribute("gradeTypes", gradeTypeRepository.findAllByOrderByName().stream()
                .collect(Collectors.toMap(GradeType::getName, g -> g, (a, b) -> a))
                .values().stream().sorted(java.util.Comparator.comparing(GradeType::getName)).toList());
        model.addAttribute("groupId", groupId);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("gradeTypeId", gradeTypeId);

        LocalDate from = parseDate(dateFrom);
        LocalDate to = parseDate(dateTo);
        if (from == null) from = startOfCurrentSemester();
        if (to == null) to = LocalDate.now();
        model.addAttribute("dateFrom", from.toString());
        model.addAttribute("dateTo", to.toString());

        Long teacherId = getCurrentTeacherIdOrNull();
        Pageable pageable = PageRequest.of(Math.max(0, page), JOURNAL_PAGE_SIZE);
        Page<LessonRecordDto> lessonPage = teacherJournalService.getLessonRecords(teacherId, from, to, groupId, subjectId, gradeTypeId, pageable);
        model.addAttribute("lessonPage", lessonPage);
        model.addAttribute("lessons", lessonPage.getContent());

        return "web/teacher/journal";
    }

    /** Страница добавления записи: выбор группы, предмета, типа, даты; после выбора группы — список студентов. */
    @GetMapping("/add")
    @Transactional(readOnly = true)
    public String addForm(@RequestParam(required = false) String groupId,
                          @RequestParam(required = false) String subjectId,
                          @RequestParam(required = false) String gradeTypeId,
                          @RequestParam(required = false) String lessonDate,
                          Model model) {
        Long groupIdLong = parseLongOrNull(groupId);
        Long subjectIdLong = parseLongOrNull(subjectId);
        Long gradeTypeIdLong = parseLongOrNull(gradeTypeId);
        addUserInfo(model);
        model.addAttribute("pageTitle", "Добавить запись в журнал");
        model.addAttribute("activeSection", "journal");
        model.addAttribute("activeTab", "journal");
        model.addAttribute("groups", getGroupsForCurrentUser());
        model.addAttribute("subjects", getSubjectsForCurrentUser());
        model.addAttribute("gradeTypes", gradeTypeRepository.findAllByOrderByName().stream()
                .collect(Collectors.toMap(GradeType::getName, g -> g, (a, b) -> a))
                .values().stream().sorted(java.util.Comparator.comparing(GradeType::getName)).toList());
        model.addAttribute("groupId", groupIdLong);
        model.addAttribute("subjectId", subjectIdLong);
        model.addAttribute("gradeTypeId", gradeTypeIdLong);
        LocalDate date = parseDate(lessonDate);
        if (date == null) date = LocalDate.now();
        model.addAttribute("lessonDate", date.toString());
        if (groupIdLong != null) {
            model.addAttribute("students", studentRepository.findByGroupIdWithUser(groupIdLong));
        } else {
            model.addAttribute("students", List.<Student>of());
        }
        int maxPoints = 40;
        if (gradeTypeIdLong != null) {
            gradeTypeRepository.findById(gradeTypeIdLong).ifPresent(gt -> {
                if (gt.getMaxScore() != null) model.addAttribute("maxPoints", gt.getMaxScore());
            });
        }
        if (!model.containsAttribute("maxPoints")) model.addAttribute("maxPoints", 40);
        return "web/teacher/journal-add";
    }

    private static Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    @PostMapping("/add")
    @Transactional
    public String addRecord(@RequestParam(required = false) String groupId,
                            @RequestParam(required = false) String subjectId,
                            @RequestParam(required = false) String gradeTypeId,
                            @RequestParam String lessonDate,
                            @RequestParam List<Long> studentIds,
                            @RequestParam(required = false) List<Long> presentIds,
                            @RequestParam(required = false) List<Integer> points,
                            RedirectAttributes ra) {
        Long groupIdLong = parseLongOrNull(groupId);
        Long subjectIdLong = parseLongOrNull(subjectId);
        Long gradeTypeIdLong = parseLongOrNull(gradeTypeId);
        if (groupIdLong == null || subjectIdLong == null || gradeTypeIdLong == null) {
            ra.addFlashAttribute("error", "Укажите группу, дисциплину и тип занятия.");
            return "redirect:/web/teacher/journal/add";
        }
        LocalDate date = LocalDate.parse(lessonDate);
        int semester = date.getMonthValue() <= 6 ? 2 : 1;
        int year = date.getYear();
        if (presentIds == null) presentIds = List.of();
        if (points == null) points = List.of();
        Long assignmentId = resolveAssignmentId(groupIdLong, subjectIdLong);
        if (assignmentId == null) {
            ra.addFlashAttribute("error", "Назначение для этой группы и дисциплины не найдено.");
            return "redirect:/web/teacher/journal/add?groupId=" + groupIdLong + "&subjectId=" + subjectIdLong + "&gradeTypeId=" + gradeTypeIdLong + "&lessonDate=" + lessonDate;
        }
        GradeType type = gradeTypeRepository.findById(gradeTypeIdLong).orElse(null);
        int maxPoints = type != null && type.getMaxScore() != null ? type.getMaxScore() : 40;
        String typeName = type != null && type.getName() != null ? type.getName() : "занятие";
        for (int i = 0; i < studentIds.size(); i++) {
            Long studentId = studentIds.get(i);
            boolean present = presentIds.contains(studentId);
            int raw = (i < points.size()) ? points.get(i) : 0;
            if (raw < 0) {
                ra.addFlashAttribute("error", "Баллы не могут быть отрицательными.");
                return "redirect:/web/teacher/journal/add?groupId=" + groupIdLong + "&subjectId=" + subjectIdLong + "&gradeTypeId=" + gradeTypeIdLong + "&lessonDate=" + lessonDate;
            }
            if (raw > maxPoints) {
                ra.addFlashAttribute("error", "Баллы не могут превышать " + maxPoints + " для типа занятия «" + typeName + "».");
                return "redirect:/web/teacher/journal/add?groupId=" + groupIdLong + "&subjectId=" + subjectIdLong + "&gradeTypeId=" + gradeTypeIdLong + "&lessonDate=" + lessonDate;
            }
            BigDecimal value = BigDecimal.valueOf(present ? raw : 0);
            attendanceService.mark(studentId, subjectIdLong, assignmentId, date, present, semester, year, null);
            CreateGradeRequest req = new CreateGradeRequest();
            req.setStudentId(studentId);
            req.setSubjectId(subjectIdLong);
            req.setAssignmentId(assignmentId);
            req.setGradeTypeId(gradeTypeIdLong);
            req.setGradeValue(value);
            req.setGradeDate(date);
            req.setSemester(semester);
            req.setAcademicYear(year);
            try {
                gradeService.createGrade(req);
            } catch (Exception ex) {
                ra.addFlashAttribute("error", "Ошибка: " + ex.getMessage());
                return "redirect:/web/teacher/journal/add?groupId=" + groupIdLong + "&subjectId=" + subjectIdLong + "&gradeTypeId=" + gradeTypeIdLong + "&lessonDate=" + lessonDate;
            }
        }
        ra.addFlashAttribute("message", "Запись сохранена");
        return "redirect:/web/teacher/journal";
    }

    /** Страница редактирования занятия: дата, группа, дисциплина, тип — из запроса; форма как в добавлении, с текущими значениями. */
    @GetMapping("/edit")
    @Transactional(readOnly = true)
    public String editForm(@RequestParam String date,
                           @RequestParam Long groupId,
                           @RequestParam Long subjectId,
                           @RequestParam Long gradeTypeId,
                           Model model) {
        LocalDate lessonDate = parseDate(date);
        if (lessonDate == null) {
            return "redirect:/web/teacher/journal";
        }
        Long assignmentId = resolveAssignmentId(groupId, subjectId);
        if (assignmentId == null) {
            return "redirect:/web/teacher/journal";
        }
        List<Student> students = studentRepository.findByGroupIdWithUser(groupId);
        if (students.isEmpty()) {
            return "redirect:/web/teacher/journal";
        }
        List<Grade> lessonGrades = gradeRepository.findByAssignmentIdInAndGradeDateBetween(Collections.singletonList(assignmentId), lessonDate, lessonDate).stream()
                .filter(g -> gradeTypeId.equals(g.getGradeType() != null ? g.getGradeType().getTypeId() : null))
                .toList();
        List<Attendance> lessonAtt = attendanceRepository.findByAssignmentIdInAndLessonDateBetween(Collections.singletonList(assignmentId), lessonDate, lessonDate);

        Map<Long, Boolean> presentMap = new HashMap<>();
        for (Attendance a : lessonAtt) {
            if (a.getStudent() != null) {
                presentMap.put(a.getStudent().getStudentId(), Boolean.TRUE.equals(a.getPresent()));
            }
        }
        Map<Long, BigDecimal> pointsMap = new HashMap<>();
        Map<Long, Long> gradeIdMap = new HashMap<>();
        for (Grade g : lessonGrades) {
            if (g.getStudent() != null) {
                pointsMap.put(g.getStudent().getStudentId(), g.getGradeValue() != null ? g.getGradeValue() : BigDecimal.ZERO);
                gradeIdMap.put(g.getStudent().getStudentId(), g.getGradeId());
            }
        }
        addUserInfo(model);
        model.addAttribute("pageTitle", "Редактировать запись в журнале");
        model.addAttribute("activeSection", "journal");
        model.addAttribute("activeTab", "journal");
        model.addAttribute("isEdit", true);
        model.addAttribute("groupId", groupId);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("gradeTypeId", gradeTypeId);
        model.addAttribute("lessonDate", lessonDate.toString());
        model.addAttribute("students", students);
        model.addAttribute("presentMap", presentMap);
        model.addAttribute("pointsMap", pointsMap);
        model.addAttribute("gradeIdMap", gradeIdMap);
        model.addAttribute("groups", getGroupsForCurrentUser());
        model.addAttribute("subjects", getSubjectsForCurrentUser());
        model.addAttribute("gradeTypes", gradeTypeRepository.findAllByOrderByName().stream()
                .collect(Collectors.toMap(GradeType::getName, g -> g, (a, b) -> a))
                .values().stream().sorted(Comparator.comparing(GradeType::getName)).toList());
        int maxPoints = 40;
        gradeTypeRepository.findById(gradeTypeId).ifPresent(gt -> {
            if (gt.getMaxScore() != null) model.addAttribute("maxPoints", gt.getMaxScore());
        });
        if (!model.containsAttribute("maxPoints")) model.addAttribute("maxPoints", 40);
        return "web/teacher/journal-edit";
    }

    @PostMapping("/edit")
    @Transactional
    public String editRecord(@RequestParam String date,
                             @RequestParam Long groupId,
                             @RequestParam Long subjectId,
                             @RequestParam Long gradeTypeId,
                             @RequestParam List<Long> studentIds,
                             @RequestParam(required = false) List<Long> presentIds,
                             @RequestParam(required = false) List<Integer> points,
                             @RequestParam(required = false) List<String> gradeIds,
                             RedirectAttributes ra) {
        LocalDate lessonDate = parseDate(date);
        if (lessonDate == null) {
            ra.addFlashAttribute("error", "Неверная дата.");
            return "redirect:/web/teacher/journal";
        }
        Long assignmentId = resolveAssignmentId(groupId, subjectId);
        if (assignmentId == null) {
            ra.addFlashAttribute("error", "Назначение не найдено.");
            return "redirect:/web/teacher/journal";
        }
        if (presentIds == null) presentIds = List.of();
        if (points == null) points = List.of();
        List<Long> parsedGradeIds = new ArrayList<>();
        if (gradeIds != null) {
            for (String g : gradeIds) {
                parsedGradeIds.add(parseLongOrNull(g));
            }
        }
        while (parsedGradeIds.size() < studentIds.size()) parsedGradeIds.add(null);
        int semester = lessonDate.getMonthValue() <= 6 ? 2 : 1;
        int year = lessonDate.getYear();
        GradeType type = gradeTypeRepository.findById(gradeTypeId).orElse(null);
        int maxPoints = type != null && type.getMaxScore() != null ? type.getMaxScore() : 40;
        String typeName = type != null ? type.getName() : "занятие";
        for (int i = 0; i < studentIds.size(); i++) {
            Long studentId = studentIds.get(i);
            boolean present = presentIds.contains(studentId);
            int raw = (i < points.size()) ? points.get(i) : 0;
            if (raw < 0 || raw > maxPoints) {
                ra.addFlashAttribute("error", raw < 0 ? "Баллы не могут быть отрицательными." : "Баллы не могут превышать " + maxPoints + " для типа «" + typeName + "».");
                return "redirect:/web/teacher/journal/edit?date=" + date + "&groupId=" + groupId + "&subjectId=" + subjectId + "&gradeTypeId=" + gradeTypeId;
            }
            BigDecimal value = BigDecimal.valueOf(present ? raw : 0);
            attendanceService.mark(studentId, subjectId, assignmentId, lessonDate, present, semester, year, null);
            Long gradeId = (i < parsedGradeIds.size()) ? parsedGradeIds.get(i) : null;
            CreateGradeRequest req = new CreateGradeRequest();
            req.setStudentId(studentId);
            req.setSubjectId(subjectId);
            req.setAssignmentId(assignmentId);
            req.setGradeTypeId(gradeTypeId);
            req.setGradeValue(value);
            req.setGradeDate(lessonDate);
            req.setSemester(semester);
            req.setAcademicYear(year);
            try {
                if (gradeId != null && gradeId > 0) {
                    gradeService.updateGrade(gradeId, req);
                } else {
                    gradeService.createGrade(req);
                }
            } catch (Exception ex) {
                ra.addFlashAttribute("error", "Ошибка: " + ex.getMessage());
                return "redirect:/web/teacher/journal/edit?date=" + date + "&groupId=" + groupId + "&subjectId=" + subjectId + "&gradeTypeId=" + gradeTypeId;
            }
        }
        ra.addFlashAttribute("message", "Запись обновлена");
        return "redirect:/web/teacher/journal";
    }

    private Long getCurrentTeacherIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) return null;
        return teacherRepository.findByUserId(principal.getId()).map(Teacher::getTeacherId).orElse(null);
    }

    private List<Long> getAssignmentIdsForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) return List.of();
        var teacherOpt = teacherRepository.findByUserId(principal.getId());
        if (teacherOpt.isPresent()) {
            return assignedCourseRepository.findByTeacherTeacherId(teacherOpt.get().getTeacherId()).stream()
                    .map(AssignedCourse::getAssignmentId).toList();
        }
        return assignedCourseRepository.findAll().stream().map(AssignedCourse::getAssignmentId).toList();
    }

    /** Для преподавателя — только группы из его назначений; для админа/деканата — все. */
    private List<StudyGroup> getGroupsForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) return groupRepository.findAllWithFaculty();
        var teacherOpt = teacherRepository.findByUserId(principal.getId());
        if (teacherOpt.isPresent()) {
            List<Long> groupIds = assignedCourseRepository.findByTeacherTeacherId(teacherOpt.get().getTeacherId()).stream()
                    .map(ac -> ac.getGroup().getGroupId()).distinct().toList();
            if (groupIds.isEmpty()) return List.of();
            return groupRepository.findByIdInWithFaculty(groupIds);
        }
        return groupRepository.findAllWithFaculty();
    }

    /** Для преподавателя — только дисциплины из его назначений; для админа/деканата — все. */
    private List<Subject> getSubjectsForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) return subjectRepository.findAllOrderByName();
        var teacherOpt = teacherRepository.findByUserId(principal.getId());
        if (teacherOpt.isPresent()) {
            return assignedCourseRepository.findByTeacherTeacherId(teacherOpt.get().getTeacherId()).stream()
                    .map(AssignedCourse::getSubject)
                    .collect(Collectors.toMap(Subject::getSubjectId, s -> s, (a, b) -> a))
                    .values().stream().sorted(java.util.Comparator.comparing(Subject::getName)).collect(Collectors.toList());
        }
        return subjectRepository.findAllOrderByName();
    }

    private static final DateTimeFormatter DD_MM_YYYY = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static LocalDate parseDate(String s) {
        if (s == null || (s = s.trim()).isBlank()) return null;
        try { return LocalDate.parse(s); } catch (DateTimeParseException e1) {
            try { return LocalDate.parse(s, DD_MM_YYYY); } catch (DateTimeParseException e2) { return null; }
        }
    }

    private static LocalDate startOfCurrentSemester() {
        LocalDate now = LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();
        if (month >= 1 && month <= 6) return LocalDate.of(year, 1, 1);
        return LocalDate.of(year, 9, 1);
    }

    private Long resolveAssignmentId(Long groupId, Long subjectId) {
        if (groupId == null || subjectId == null) return null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) return null;
        var teacherOpt = teacherRepository.findByUserId(principal.getId());
        if (teacherOpt.isEmpty()) return null;
        return assignedCourseRepository.findByTeacherIdAndGroupIdAndSubjectId(
                teacherOpt.get().getTeacherId(), groupId, subjectId)
                .map(AssignedCourse::getAssignmentId).orElse(null);
    }

    private void addUserInfo(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
        }
    }
}
