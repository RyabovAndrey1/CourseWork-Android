package ru.ryabov.studentperformance.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.entity.StudyGroup;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.GroupRepository;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.repository.UserRepository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * Дозаполнение БД: если студентов меньше 13, создаёт пользователей student4..student13
 * и записи студентов (для выбора в фильтрах отчётов и т.д.).
 * Работает и при H2 (Flyway отключён), и при PostgreSQL (если V9 не выполнялась).
 */
@Component
@Order(100)
public class ExtraSeedDataLoader implements ApplicationRunner {

    private static final String SEED_PASSWORD = "password123";

    private static final List<StudentSeed> EXTRA_STUDENTS = List.of(
            new StudentSeed("student4", "student4@mi.vlgu.ru", "Козлов", "Дмитрий", "Александрович", 2, "ПИН-122-004", 2022, "2004-01-10"),
            new StudentSeed("student5", "student5@mi.vlgu.ru", "Новикова", "Елена", "Игоревна", 2, "ПИН-122-005", 2022, "2003-07-05"),
            new StudentSeed("student6", "student6@mi.vlgu.ru", "Морозов", "Андрей", "Викторович", 1, "ПИН-121-002", 2021, "2002-09-20"),
            new StudentSeed("student7", "student7@mi.vlgu.ru", "Волкова", "Ольга", "Сергеевна", 1, "ПИН-121-003", 2021, "2003-02-14"),
            new StudentSeed("student8", "student8@mi.vlgu.ru", "Соколов", "Максим", "Петрович", 2, "ПИН-122-006", 2022, "2004-04-08"),
            new StudentSeed("student9", "student9@mi.vlgu.ru", "Лебедева", "Татьяна", "Андреевна", 2, "ПИН-122-007", 2022, "2003-11-30"),
            new StudentSeed("student10", "student10@mi.vlgu.ru", "Кузнецов", "Илья", "Дмитриевич", 1, "ПИН-121-004", 2021, "2002-06-12"),
            new StudentSeed("student11", "student11@mi.vlgu.ru", "Попова", "Наталья", "Олеговна", 2, "ПИН-122-008", 2022, "2004-03-25"),
            new StudentSeed("student12", "student12@mi.vlgu.ru", "Васильев", "Роман", "Иванович", 1, "ПИН-121-005", 2021, "2002-12-01"),
            new StudentSeed("student13", "student13@mi.vlgu.ru", "Михайлова", "Светлана", "Николаевна", 2, "ПИН-122-009", 2022, "2003-10-18")
    );

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private GroupRepository groupRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (studentRepository.count() >= 13) {
            return;
        }
        List<StudyGroup> groups = groupRepository.findAllWithFaculty().stream()
                .sorted(Comparator.comparing(StudyGroup::getGroupId))
                .limit(2)
                .toList();
        if (groups.size() < 2) {
            return;
        }
        StudyGroup group1 = groups.get(0);
        StudyGroup group2 = groups.get(1);
        String encoded = passwordEncoder.encode(SEED_PASSWORD);
        for (StudentSeed seed : EXTRA_STUDENTS) {
            if (userRepository.findByLogin(seed.login).isPresent()) {
                continue;
            }
            User user = new User(seed.login, encoded, seed.email, seed.lastName, seed.firstName, seed.middleName, User.Role.STUDENT);
            user = userRepository.saveAndFlush(user);
            StudyGroup group = seed.groupOrder == 1 ? group1 : group2;
            LocalDate birthDate = LocalDate.parse(seed.birthDate);
            Student student = new Student(user, group, seed.recordBook, seed.admissionYear, birthDate, "+79001110000", "г. Муром");
            studentRepository.saveAndFlush(student);
        }
    }

    private static class StudentSeed {
        final String login, email, lastName, firstName, middleName;
        final int groupOrder;
        final String recordBook;
        final int admissionYear;
        final String birthDate;

        StudentSeed(String login, String email, String lastName, String firstName, String middleName,
                    int groupOrder, String recordBook, int admissionYear, String birthDate) {
            this.login = login;
            this.email = email;
            this.lastName = lastName;
            this.firstName = firstName;
            this.middleName = middleName;
            this.groupOrder = groupOrder;
            this.recordBook = recordBook;
            this.admissionYear = admissionYear;
            this.birthDate = birthDate;
        }
    }
}
