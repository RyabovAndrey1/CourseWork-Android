package ru.ryabov.studentperformance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.studentperformance.entity.*;
import ru.ryabov.studentperformance.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Расширение БД: добавляет факультеты, кафедры, преподавателей, дисциплины, группы и назначения,
 * если в БД только начальный сид (3 факультета, 4 дисциплины). Работает с H2 и PostgreSQL.
 */
@Component
@Order(200)
public class ExpandSeedDataLoader implements ApplicationRunner {

    private static final String SEED_PASSWORD = "password123";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private FacultyRepository facultyRepository;
    @Autowired
    private DepartmentRepository departmentRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private TeacherRepository teacherRepository;
    @Autowired
    private AssignedCourseRepository assignedCourseRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (facultyRepository.count() != 3 || subjectRepository.count() != 4) {
            return;
        }
        String encoded = passwordEncoder.encode(SEED_PASSWORD);
        List<Faculty> faculties = facultyRepository.findAllByOrderByName();
        if (faculties.size() < 3) return;
        Faculty f1 = faculties.get(0);
        Faculty f2 = faculties.get(1);
        Faculty f3 = faculties.get(2);

        // Доп. факультеты
        Faculty f4 = saveFacultyIfAbsent("Юридический", "Федоров К.Л.");
        Faculty f5 = saveFacultyIfAbsent("Математики и физики", "Николаев В.П.");

        // Доп. кафедры
        saveDepartmentIfAbsent("Информационной безопасности", f1, "Орлов С.Н.");
        saveDepartmentIfAbsent("Менеджмента", f2, "Волкова Т.И.");
        saveDepartmentIfAbsent("Гражданского права", f4 != null ? f4 : f3, "Соколова Е.А.");
        saveDepartmentIfAbsent("Прикладной математики", f5 != null ? f5 : f3, "Павлов Д.В.");
        List<Department> deps = departmentRepository.findAllByOrderByName();
        Department d1 = deps.isEmpty() ? null : deps.get(0);
        Department d2 = deps.size() > 1 ? deps.get(1) : d1;

        // Доп. преподаватели (teacher3, teacher4, teacher5)
        User teacher3User = userRepository.findByLogin("teacher3").orElseGet(() -> {
            User u = new User("teacher3", encoded, "teacher3@mi.vlgu.ru", "Кузнецов", "Сергей", "Андреевич", User.Role.TEACHER);
            return userRepository.saveAndFlush(u);
        });
        User teacher4User = userRepository.findByLogin("teacher4").orElseGet(() -> {
            User u = new User("teacher4", encoded, "teacher4@mi.vlgu.ru", "Федорова", "Елена", "Викторовна", User.Role.TEACHER);
            return userRepository.saveAndFlush(u);
        });
        User teacher5User = userRepository.findByLogin("teacher5").orElseGet(() -> {
            User u = new User("teacher5", encoded, "teacher5@mi.vlgu.ru", "Михайлов", "Павел", "Игоревич", User.Role.TEACHER);
            return userRepository.saveAndFlush(u);
        });
        deps = departmentRepository.findAllByOrderByName();
        Department dep1 = deps.isEmpty() ? null : deps.get(0);
        if (dep1 != null) {
            saveTeacherIfAbsent(teacher3User, dep1, "Кандидат технических наук", "Старший преподаватель");
            saveTeacherIfAbsent(teacher4User, dep1, "Доктор экономических наук", "Профессор");
            saveTeacherIfAbsent(teacher5User, deps.size() > 2 ? deps.get(2) : dep1, "Кандидат физико-математических наук", "Доцент");
        }

        // Доп. дисциплины
        saveSubjectIfAbsent("Информационная безопасность", "IB", "3.0", Subject.ControlType.EXAM);
        saveSubjectIfAbsent("Математический анализ", "MA", "6.0", Subject.ControlType.EXAM);
        saveSubjectIfAbsent("Линейная алгебра", "LA", "4.0", Subject.ControlType.EXAM);
        saveSubjectIfAbsent("Теория вероятностей", "TV", "3.0", Subject.ControlType.EXAM);
        saveSubjectIfAbsent("Менеджмент", "MNG", "4.0", Subject.ControlType.CREDIT);
        saveSubjectIfAbsent("Правоведение", "PRAV", "2.0", Subject.ControlType.CREDIT);

        // Доп. группы
        saveGroupIfAbsent("ПИН-124", f1, 2024, "Программная инженерия");
        saveGroupIfAbsent("ИБ-101", f1, 2023, "Информационная безопасность");
        saveGroupIfAbsent("ЭКН-102", f2, 2024, "Экономика");
        saveGroupIfAbsent("ЮР-101", f4 != null ? f4 : f3, 2023, "Юриспруденция");
        saveGroupIfAbsent("МФ-101", f5 != null ? f5 : f3, 2023, "Прикладная математика");
    }

    private Faculty saveFacultyIfAbsent(String name, String deanName) {
        if (facultyRepository.findByName(name) != null) return facultyRepository.findByName(name);
        Faculty f = new Faculty(name, deanName);
        f.setCreatedAt(LocalDateTime.now());
        return facultyRepository.saveAndFlush(f);
    }

    private Department saveDepartmentIfAbsent(String name, Faculty faculty, String headName) {
        List<Department> existing = departmentRepository.findByFacultyFacultyId(faculty.getFacultyId());
        if (existing.stream().anyMatch(d -> name.equals(d.getName()))) return existing.stream().filter(d -> name.equals(d.getName())).findFirst().orElse(null);
        Department d = new Department(name, faculty, headName);
        d.setCreatedAt(LocalDateTime.now());
        return departmentRepository.saveAndFlush(d);
    }

    private void saveTeacherIfAbsent(User user, Department department, String degree, String position) {
        if (teacherRepository.findByUserId(user.getUserId()).isPresent()) return;
        Teacher t = new Teacher(user, department, degree, position);
        t.setCreatedAt(LocalDateTime.now());
        teacherRepository.saveAndFlush(t);
    }

    private void saveSubjectIfAbsent(String name, String code, String credits, Subject.ControlType controlType) {
        if (subjectRepository.findByCode(code) != null) return;
        Subject s = new Subject(name, code, new BigDecimal(credits), 144, 36, 54, 54, controlType, null);
        s.setCreatedAt(LocalDateTime.now());
        subjectRepository.saveAndFlush(s);
    }

    private void saveGroupIfAbsent(String name, Faculty faculty, int year, String spec) {
        List<StudyGroup> all = groupRepository.findAllWithFaculty();
        if (all.stream().anyMatch(g -> name.equals(g.getName()))) return;
        StudyGroup g = new StudyGroup(name, faculty, year, spec);
        g.setCreatedAt(LocalDateTime.now());
        groupRepository.saveAndFlush(g);
    }
}
