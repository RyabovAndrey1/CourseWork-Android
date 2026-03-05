package ru.ryabov.studentperformance.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.ryabov.studentperformance.entity.ReportRecord;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.entity.Grade;
import ru.ryabov.studentperformance.repository.GradeRepository;
import ru.ryabov.studentperformance.repository.GroupRepository;
import ru.ryabov.studentperformance.repository.ReportRecordRepository;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.repository.SubjectRepository;
import ru.ryabov.studentperformance.security.UserPrincipal;
import ru.ryabov.studentperformance.service.EmailService;
import ru.ryabov.studentperformance.service.ReportService;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.entity.StudyGroup;
import ru.ryabov.studentperformance.entity.Subject;

/**
 * Выдача отчётов в Excel и PDF (требование курсовой: отчёты в виде файлов).
 * Студент может скачать только свой отчёт; остальные роли — по своим правам.
 */
@Controller
@RequestMapping("/web/reports")
public class WebReportsController {

    @Autowired
    private ReportService reportService;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired(required = false)
    private EmailService emailService;

    @Autowired
    private ReportRecordRepository reportRecordRepository;

    @Autowired
    private GradeRepository gradeRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Список сформированных отчётов с фильтрами (по типу, группе, предмету, студенту; без даты формирования и периода). */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    @Transactional(readOnly = true)
    public String index(@RequestParam(required = false) String reportType,
                        @RequestParam(required = false) String groupIdStr,
                        @RequestParam(required = false) String subjectIdStr,
                        @RequestParam(required = false) String studentIdStr,
                        Model model) {
        Long groupId = parseLongOrNull(groupIdStr);
        Long subjectId = parseLongOrNull(subjectIdStr);
        Long studentId = parseLongOrNull(studentIdStr);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
            model.addAttribute("activeSection", "reports");
            model.addAttribute("activeTab", "reports");
        }
        Long userId = auth != null && auth.getPrincipal() instanceof UserPrincipal p ? p.getId() : null;
        String role = auth != null && auth.getPrincipal() instanceof UserPrincipal pr ? pr.getRole() : null;
        boolean isStudent = "STUDENT".equals(role);
        List<ReportRecord> all = userId != null ? reportRecordRepository.findByUserIdOrderByCreatedAtDesc(userId) : List.of();
        List<ReportRecord> filtered = isStudent ? all : filterRecords(all, reportType, groupId, subjectId, studentId);
        if (isStudent && subjectId != null) {
            filtered = filtered.stream().filter(r -> subjectId.equals(r.getSubjectId())).toList();
        }
        List<ReportListRow> rows = new ArrayList<>();
        for (ReportRecord r : filtered) {
            String desc = formatReportDescription(r);
            String periodStr = formatPeriod(r.getPeriodFrom(), r.getPeriodTo());
            String createdStr = r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_FMT) : "—";
            rows.add(new ReportListRow(r.getId(), r.getReportType(), desc, periodStr, createdStr, r.getFormat()));
        }
        model.addAttribute("reportList", rows);
        model.addAttribute("isStudent", isStudent);
        model.addAttribute("reportType", reportType);
        model.addAttribute("groupId", groupId);
        model.addAttribute("subjectId", subjectId);
        model.addAttribute("studentId", studentId);
        model.addAttribute("subjects", subjectRepository.findAllOrderByName().stream().map(s -> new SubjectReportRow(s.getSubjectId(), s.getName())).collect(Collectors.toList()));
        model.addAttribute("groups", isStudent ? List.of() : groupRepository.findAllWithFaculty().stream().map(g -> new GroupReportRow(g.getGroupId(), g.getName() + (g.getFaculty() != null ? " (" + g.getFaculty().getName() + ")" : ""))).collect(Collectors.toList()));
        model.addAttribute("students", isStudent ? List.of() : studentRepository.findAll().stream().map(s -> new StudentReportRow(s.getStudentId(), s.getFullName() != null ? s.getFullName() : "—")).collect(Collectors.toList()));
        return "web/reports/index";
    }

    private List<ReportRecord> filterRecords(List<ReportRecord> all, String reportType, Long groupId, Long subjectId, Long studentId) {
        return all.stream()
                .filter(r -> reportType == null || reportType.isBlank() || reportType.equalsIgnoreCase(r.getReportType()))
                .filter(r -> groupId == null || (r.getGroupId() != null && r.getGroupId().equals(groupId)))
                .filter(r -> subjectId == null || (r.getSubjectId() != null && r.getSubjectId().equals(subjectId)))
                .filter(r -> studentId == null || (r.getStudentId() != null && r.getStudentId().equals(studentId)))
                .toList();
    }

    private String formatReportDescription(ReportRecord r) {
        if ("GROUP".equals(r.getReportType()) && r.getGroupId() != null) {
            return "По группе: " + groupRepository.findById(r.getGroupId()).map(g -> g.getName()).orElse("id " + r.getGroupId());
        }
        if ("SUBJECT".equals(r.getReportType()) && r.getSubjectId() != null) {
            return "По предмету: " + subjectRepository.findById(r.getSubjectId()).map(s -> s.getName()).orElse("id " + r.getSubjectId());
        }
        if ("STUDENT".equals(r.getReportType()) && r.getStudentId() != null) {
            var st = studentRepository.findById(r.getStudentId());
            String name = st.map(s -> s.getFullName()).orElse("id " + r.getStudentId());
            String groupName = st.flatMap(s -> s.getGroup() != null ? groupRepository.findById(s.getGroup().getGroupId()).map(g -> g.getName()) : java.util.Optional.empty()).orElse("");
            return "По студенту: " + (groupName.isEmpty() ? "" : groupName + ", ") + name;
        }
        return "—";
    }

    private String formatPeriod(LocalDate from, LocalDate to) {
        if (from == null && to == null) return "—";
        if (from == null) return "— " + (to != null ? to.format(DATE_FMT) : "");
        if (to == null) return from.format(DATE_FMT) + " —";
        return from.format(DATE_FMT) + " — " + to.format(DATE_FMT);
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s); } catch (Exception e) { return null; }
    }

    public record ReportListRow(Long id, String reportType, String description, String period, String createdAt, String format) {}

    /** Страница создания отчёта. Для преподавателей — полная форма; для студента — только период, предмет, формат. */
    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    @Transactional(readOnly = true)
    public String createForm(@RequestParam(required = false) String groupId,
                             @RequestParam(required = false) String reportType,
                             Model model) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            model.addAttribute("userName", principal.getUsername());
            model.addAttribute("userRole", principal.getRole());
            model.addAttribute("activeSection", "reports");
            model.addAttribute("activeTab", "reports");
            if ("STUDENT".equals(principal.getRole())) {
                var studentOpt = studentRepository.findByUserId(principal.getId());
                if (studentOpt.isEmpty()) {
                    model.addAttribute("error", "Нет привязки к записи студента.");
                    model.addAttribute("studentSubjects", List.<SubjectReportRow>of());
                    return "web/reports/create-student";
                }
                List<Grade> grades = gradeRepository.findByStudentIdOrderByDateDescWithSubject(studentOpt.get().getStudentId());
                List<SubjectReportRow> studentSubjects = grades.stream()
                        .map(Grade::getSubject)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toMap(Subject::getSubjectId, s -> s, (a, b) -> a))
                        .values().stream()
                        .sorted((a, b) -> (a.getName() != null ? a.getName() : "").compareToIgnoreCase(b.getName() != null ? b.getName() : ""))
                        .map(s -> new SubjectReportRow(s.getSubjectId(), s.getName()))
                        .collect(Collectors.toList());
                model.addAttribute("studentSubjects", studentSubjects);
                return "web/reports/create-student";
            }
        }
        Long groupIdLong = parseLongOrNull(groupId);
        model.addAttribute("subjects", subjectRepository.findAllOrderByName().stream().map(s -> new SubjectReportRow(s.getSubjectId(), s.getName())).collect(Collectors.toList()));
        model.addAttribute("groups", groupRepository.findAllWithFaculty().stream().map(g -> new GroupReportRow(g.getGroupId(), g.getName() + (g.getFaculty() != null ? " (" + g.getFaculty().getName() + ")" : ""))).collect(Collectors.toList()));
        List<StudentReportRow> students;
        if (groupIdLong != null) {
            students = studentRepository.findByGroupIdWithUser(groupIdLong).stream()
                    .map(s -> new StudentReportRow(s.getStudentId(), s.getFullName() != null ? s.getFullName() : "—")).collect(Collectors.toList());
        } else {
            students = studentRepository.findAll().stream().map(s -> new StudentReportRow(s.getStudentId(), s.getFullName() != null ? s.getFullName() : "—")).collect(Collectors.toList());
        }
            model.addAttribute("students", students);
        model.addAttribute("selectedGroupId", groupIdLong);
        model.addAttribute("selectedReportType", reportType);
        return "web/reports/create";
    }

    private static Long parseLongOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    /** Транслитерация кириллицы в латиницу для имени файла (явная карта, 1:1). */
    private static final java.util.Map<Character, Character> CYR_TO_LAT = new java.util.HashMap<>();
    static {
        String cyr = "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя";
        String lat = "ABVGDEEZZIJKLMNOPRSTUFHCSSYYEYYYabvgdeezzijklmnoprstufhcssyyeyyy";
        for (int i = 0; i < cyr.length() && i < lat.length(); i++) {
            CYR_TO_LAT.put(cyr.charAt(i), lat.charAt(i));
        }
    }

    private static String slug(String s) {
        if (s == null || s.isBlank()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            Character mapped = CYR_TO_LAT.get(c);
            if (mapped != null) sb.append(mapped);
            else if (Character.isLetterOrDigit(c) && c < 128) sb.append(c);
            else if (c == ' ' || c == '-') sb.append('_');
        }
        return sb.toString().replace("__", "_").replaceAll("_+$|^_+", "");
    }

    /** Имя файла по содержимому: otchet_BD, otchet_PIN121, otchet_PetrovaAI_PIN122 */
    private String buildReportFileName(String reportType, Long subjectId, Long groupId, Long studentId, String format) {
        String ext = "EXCEL".equalsIgnoreCase(format) ? ".xlsx" : ".pdf";
        String base = "otchet_";
        if ("SUBJECT".equalsIgnoreCase(reportType) && subjectId != null) {
            String name = subjectRepository.findById(subjectId).map(Subject::getName).orElse("subject" + subjectId);
            String abbr = java.util.Arrays.stream(name.split("\\s+")).filter(w -> !w.isEmpty())
                    .map(w -> { String t = slug(w); return t.isEmpty() ? "" : String.valueOf(Character.toUpperCase(t.charAt(0))); })
                    .reduce("", (a, b) -> a + b);
            if (abbr.isEmpty()) abbr = slug(name);
            if (abbr.isEmpty()) abbr = "subject" + subjectId;
            return base + abbr + ext;
        }
        if ("GROUP".equalsIgnoreCase(reportType) && groupId != null) {
            String name = groupRepository.findById(groupId).map(StudyGroup::getName).orElse("group" + groupId);
            String g = slug(name);
            if (g.isEmpty()) g = "group" + groupId;
            return base + g + ext;
        }
        if ("STUDENT".equalsIgnoreCase(reportType) && studentId != null) {
            var studentOpt = studentRepository.findByIdWithUserAndGroup(studentId);
            if (studentOpt.isEmpty()) return base + "student" + studentId + ext;
            Student st = studentOpt.get();
            String part1 = "student" + studentId;
            if (st.getUser() != null) {
                String ln = slug(st.getUser().getLastName() != null ? st.getUser().getLastName() : "");
                String fn1 = st.getUser().getFirstName() != null ? slug(st.getUser().getFirstName()) : "";
                String mn1 = st.getUser().getMiddleName() != null ? slug(st.getUser().getMiddleName()) : "";
                String fn = fn1.isEmpty() ? "" : fn1.substring(0, 1);
                String mn = mn1.isEmpty() ? "" : mn1.substring(0, 1);
                part1 = ln.isEmpty() ? part1 : (ln + fn + mn);
            }
            String part2 = st.getGroup() != null ? slug(st.getGroup().getName()) : "";
            if (part2.isEmpty()) part2 = "g" + studentId;
            return base + part1 + "_" + part2 + ext;
        }
        return base + "report" + ext;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    @Transactional
    public String createReport(@RequestParam(required = false) String reportType,
                               @RequestParam(required = false) String groupId,
                               @RequestParam(required = false) String subjectId,
                               @RequestParam(required = false) String studentId,
                               @RequestParam(required = false) String periodFrom,
                               @RequestParam(required = false) String periodTo,
                               @RequestParam String format,
                               RedirectAttributes ra) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            ra.addFlashAttribute("error", "Необходима авторизация");
            return "redirect:/web/reports";
        }
        Long userId = principal.getId();
        boolean isStudent = "STUDENT".equals(principal.getRole());
        if (isStudent) {
            var studentOpt = studentRepository.findByUserId(userId);
            if (studentOpt.isEmpty()) {
                ra.addFlashAttribute("error", "Нет привязки к записи студента.");
                return "redirect:/web/reports";
            }
            LocalDate pf = parseDate(periodFrom);
            LocalDate pt = parseDate(periodTo);
            if (pf == null || pt == null) {
                ra.addFlashAttribute("error", "Укажите период (дату с и дату по).");
                return "redirect:/web/reports/create";
            }
            Long subjectIdLong = parseLongOrNull(subjectId);
            Long stId = studentOpt.get().getStudentId();
            Resource resource = "EXCEL".equalsIgnoreCase(format)
                    ? reportService.buildStudentGradesExcel(stId, pf, pt, subjectIdLong)
                    : reportService.buildStudentGradesPdf(stId, pf, pt, subjectIdLong);
            byte[] bytes;
            try (InputStream is = resource.getInputStream()) { bytes = is.readAllBytes(); } catch (Exception e) {
                ra.addFlashAttribute("error", "Ошибка формирования: " + e.getMessage());
                return "redirect:/web/reports/create";
            }
            ReportRecord record = new ReportRecord();
            record.setUserId(userId);
            record.setReportType("STUDENT");
            record.setStudentId(stId);
            record.setSubjectId(subjectIdLong);
            record.setPeriodFrom(pf);
            record.setPeriodTo(pt);
            record.setFormat(format.toUpperCase());
            record.setCreatedAt(java.time.LocalDateTime.now());
            reportRecordRepository.save(record);
            // Письмо — на почту текущего пользователя (под которым вошли)
            String emailTo = principal.getEmail();
            if (emailTo != null && !emailTo.isBlank() && emailService != null) {
                String fileName = buildReportFileName("STUDENT", null, null, stId, format);
                emailService.sendReportByEmail(emailTo.trim(), bytes, fileName, "Отчёт успеваемости");
            }
            ra.addFlashAttribute("message", "Отчёт создан и отправлен на вашу почту");
            return "redirect:/web/reports";
        }
        Long groupIdLong = parseLongOrNull(groupId);
        Long subjectIdLong = parseLongOrNull(subjectId);
        Long studentIdLong = parseLongOrNull(studentId);
        LocalDate pf = parseDate(periodFrom);
        LocalDate pt = parseDate(periodTo);
        Resource resource = null;
        Long subId = null; Long grId = null; Long stId = null;
        if ("SUBJECT".equalsIgnoreCase(reportType) && subjectIdLong != null) {
            ensureNotStudentForSubject();
            resource = "EXCEL".equalsIgnoreCase(format) ? reportService.buildSubjectGradesExcel(subjectIdLong) : reportService.buildSubjectGradesPdf(subjectIdLong);
            subId = subjectIdLong;
        } else if ("GROUP".equalsIgnoreCase(reportType) && groupIdLong != null) {
            ensureNotStudentForGroup();
            resource = "EXCEL".equalsIgnoreCase(format) ? reportService.buildGroupGradesExcel(groupIdLong) : reportService.buildGroupGradesPdf(groupIdLong);
            grId = groupIdLong;
        } else if ("STUDENT".equalsIgnoreCase(reportType) && studentIdLong != null) {
            ensureStudentCanAccessReport(studentIdLong);
            resource = "EXCEL".equalsIgnoreCase(format) ? reportService.buildStudentGradesExcel(studentIdLong) : reportService.buildStudentGradesPdf(studentIdLong);
            stId = studentIdLong;
        }
        if (resource == null) {
            ra.addFlashAttribute("error", "Укажите тип отчёта и выберите группу, предмет или студента");
            return "redirect:/web/reports/create";
        }
        byte[] bytes;
        try (InputStream is = resource.getInputStream()) { bytes = is.readAllBytes(); } catch (Exception e) {
            ra.addFlashAttribute("error", "Ошибка формирования: " + e.getMessage());
            return "redirect:/web/reports/create";
        }
        ReportRecord record = new ReportRecord();
        record.setUserId(userId);
        record.setReportType(reportType.toUpperCase());
        record.setGroupId(grId);
        record.setSubjectId(subId);
        record.setStudentId(stId);
        record.setPeriodFrom(pf);
        record.setPeriodTo(pt);
        record.setFormat(format.toUpperCase());
        record.setCreatedAt(java.time.LocalDateTime.now());
        reportRecordRepository.save(record);
        // Письмо с отчётом — на почту текущего пользователя (под которым вошли)
        String emailTo = principal.getEmail();
        if (emailTo != null && !emailTo.isBlank() && emailService != null) {
            String fileName = buildReportFileName(reportType.toUpperCase(), subId, grId, stId, format);
            emailService.sendReportByEmail(emailTo.trim(), bytes, fileName, "Отчёт успеваемости");
        }
        ra.addFlashAttribute("message", "Отчёт создан и отправлен на вашу почту");
        return "redirect:/web/reports";
    }

    @GetMapping("/download/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    @Transactional(readOnly = true)
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        ReportRecord r = reportRecordRepository.findById(id).orElse(null);
        if (r == null) return ResponseEntity.notFound().build();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            if (!r.getUserId().equals(principal.getId()) && "STUDENT".equals(principal.getRole())) {
                return ResponseEntity.status(403).build();
            }
        }
        Resource resource = null;
        if ("SUBJECT".equals(r.getReportType()) && r.getSubjectId() != null) {
            resource = "EXCEL".equals(r.getFormat()) ? reportService.buildSubjectGradesExcel(r.getSubjectId()) : reportService.buildSubjectGradesPdf(r.getSubjectId());
        } else if ("GROUP".equals(r.getReportType()) && r.getGroupId() != null) {
            resource = "EXCEL".equals(r.getFormat()) ? reportService.buildGroupGradesExcel(r.getGroupId()) : reportService.buildGroupGradesPdf(r.getGroupId());
        } else if ("STUDENT".equals(r.getReportType()) && r.getStudentId() != null) {
            ensureStudentCanAccessReport(r.getStudentId());
            resource = "EXCEL".equals(r.getFormat())
                    ? reportService.buildStudentGradesExcel(r.getStudentId(), r.getPeriodFrom(), r.getPeriodTo(), r.getSubjectId())
                    : reportService.buildStudentGradesPdf(r.getStudentId(), r.getPeriodFrom(), r.getPeriodTo(), r.getSubjectId());
        }
        if (resource == null) return ResponseEntity.notFound().build();
        String fileName = buildReportFileName(r.getReportType(), r.getSubjectId(), r.getGroupId(), r.getStudentId(), r.getFormat());
        MediaType mt = "EXCEL".equals(r.getFormat()) ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") : MediaType.APPLICATION_PDF;
        return ResponseEntity.ok().contentType(mt).header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"").body(resource);
    }

    public record SubjectReportRow(long id, String name) {}
    public record GroupReportRow(long id, String name) {}
    public record StudentReportRow(long id, String name) {}

    @GetMapping("/subject/{subjectId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<Resource> subjectExcel(@PathVariable Long subjectId) {
        var resource = reportService.buildSubjectGradesExcel(subjectId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-subject-" + subjectId + ".xlsx")
                .body(resource);
    }

    @GetMapping("/subject/{subjectId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<Resource> subjectPdf(@PathVariable Long subjectId) {
        var resource = reportService.buildSubjectGradesPdf(subjectId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-subject-" + subjectId + ".pdf")
                .body(resource);
    }

    @GetMapping("/group/{groupId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<Resource> groupExcel(@PathVariable Long groupId) {
        var resource = reportService.buildGroupGradesExcel(groupId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-group-" + groupId + ".xlsx")
                .body(resource);
    }

    @GetMapping("/group/{groupId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<Resource> groupPdf(@PathVariable Long groupId) {
        var resource = reportService.buildGroupGradesPdf(groupId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-group-" + groupId + ".pdf")
                .body(resource);
    }

    @GetMapping("/student/{studentId}/excel")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<Resource> studentExcel(@PathVariable Long studentId) {
        ensureStudentCanAccessReport(studentId);
        var resource = reportService.buildStudentGradesExcel(studentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-student-" + studentId + ".xlsx")
                .body(resource);
    }

    @GetMapping("/student/{studentId}/pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER', 'STUDENT')")
    public ResponseEntity<Resource> studentPdf(@PathVariable Long studentId) {
        ensureStudentCanAccessReport(studentId);
        var resource = reportService.buildStudentGradesPdf(studentId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=report-student-" + studentId + ".pdf")
                .body(resource);
    }

    private void ensureNotStudentForSubject() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal && "STUDENT".equals(principal.getRole())) {
            throw new AccessDeniedException("Студент может формировать только свой отчёт");
        }
    }

    private void ensureNotStudentForGroup() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal && "STUDENT".equals(principal.getRole())) {
            throw new AccessDeniedException("Студент может формировать только свой отчёт");
        }
    }

    /** Студент может скачать только отчёт по себе; ADMIN/DEANERY/TEACHER — по любому студенту. */
    private void ensureStudentCanAccessReport(Long studentId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal
                && "STUDENT".equals(principal.getRole())) {
            var currentStudent = studentRepository.findByUserId(principal.getId());
            if (currentStudent.isEmpty() || !currentStudent.get().getStudentId().equals(studentId)) {
                throw new AccessDeniedException("Студент может скачать только свой отчёт");
            }
        }
    }
}
