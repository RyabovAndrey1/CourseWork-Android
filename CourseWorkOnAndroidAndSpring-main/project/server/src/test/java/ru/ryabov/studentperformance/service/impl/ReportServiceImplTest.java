package ru.ryabov.studentperformance.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.entity.StudyGroup;
import ru.ryabov.studentperformance.entity.Subject;
import ru.ryabov.studentperformance.entity.User;
import ru.ryabov.studentperformance.repository.AttendanceRepository;
import ru.ryabov.studentperformance.repository.GradeRepository;
import ru.ryabov.studentperformance.repository.GroupRepository;
import ru.ryabov.studentperformance.repository.StudentRepository;
import ru.ryabov.studentperformance.repository.SubjectRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты ReportServiceImpl")
class ReportServiceImplTest {

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private SubjectRepository subjectRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private StudyGroup group;
    private Student student;
    private Subject subject;

    @BeforeEach
    void setUp() {
        group = new StudyGroup();
        group.setGroupId(1L);
        group.setName("ПИН-122");

        User user = new User();
        user.setUserId(1L);
        user.setLastName("Иванов");
        user.setFirstName("Петр");
        student = new Student();
        student.setStudentId(1L);
        student.setUser(user);
        student.setGroup(group);

        subject = new Subject();
        subject.setSubjectId(1L);
        subject.setName("Базы данных");
    }

    @Test
    @DisplayName("buildGroupGradesExcel возвращает непустой ресурс при существующей группе")
    void buildGroupGradesExcel_ShouldReturnResource_WhenGroupExists() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(gradeRepository.findByStudentGroupGroupIdOrderByGradeDateDesc(1L)).thenReturn(List.of());
        when(studentRepository.findByGroupIdWithUser(1L)).thenReturn(List.of());

        Resource resource = reportService.buildGroupGradesExcel(1L);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        verify(groupRepository).findById(1L);
        verify(gradeRepository).findByStudentGroupGroupIdOrderByGradeDateDesc(1L);
        verify(studentRepository).findByGroupIdWithUser(1L);
    }

    @Test
    @DisplayName("buildGroupGradesPdf возвращает непустой ресурс при существующей группе")
    void buildGroupGradesPdf_ShouldReturnResource_WhenGroupExists() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(gradeRepository.findByStudentGroupGroupIdOrderByGradeDateDesc(1L)).thenReturn(List.of());
        when(studentRepository.findByGroupIdWithUser(1L)).thenReturn(List.of());

        Resource resource = reportService.buildGroupGradesPdf(1L);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        verify(groupRepository).findById(1L);
    }

    @Test
    @DisplayName("buildStudentGradesExcel выбрасывает исключение при отсутствии студента")
    void buildStudentGradesExcel_ShouldThrow_WhenStudentNotFound() {
        when(studentRepository.findByIdWithUserAndGroup(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.buildStudentGradesExcel(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Студент не найден");

        verify(studentRepository).findByIdWithUserAndGroup(1L);
    }

    @Test
    @DisplayName("buildStudentGradesExcel возвращает ресурс при существующем студенте")
    void buildStudentGradesExcel_ShouldReturnResource_WhenStudentExists() {
        when(studentRepository.findByIdWithUserAndGroup(1L)).thenReturn(Optional.of(student));
        when(gradeRepository.findByStudentIdOrderByDateDescWithSubject(1L)).thenReturn(List.of());
        when(attendanceRepository.findByStudentStudentIdOrderByLessonDateDescWithDetails(1L)).thenReturn(List.of());

        Resource resource = reportService.buildStudentGradesExcel(1L);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        verify(studentRepository).findByIdWithUserAndGroup(1L);
        verify(gradeRepository).findByStudentIdOrderByDateDescWithSubject(1L);
    }

    @Test
    @DisplayName("buildStudentGradesPdf с фильтром по периоду возвращает ресурс")
    void buildStudentGradesPdf_WithPeriodFilter_ShouldReturnResource() {
        when(studentRepository.findByIdWithUserAndGroup(1L)).thenReturn(Optional.of(student));
        when(gradeRepository.findByStudentIdOrderByDateDescWithSubject(1L)).thenReturn(List.of());
        when(attendanceRepository.findByStudentStudentIdOrderByLessonDateDescWithDetails(1L)).thenReturn(List.of());

        LocalDate from = LocalDate.of(2024, 9, 1);
        LocalDate to = LocalDate.of(2024, 12, 31);

        Resource resource = reportService.buildStudentGradesPdf(1L, from, to, null);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        verify(studentRepository).findByIdWithUserAndGroup(1L);
    }

    @Test
    @DisplayName("buildSubjectGradesExcel возвращает ресурс при существующем предмете")
    void buildSubjectGradesExcel_ShouldReturnResource_WhenSubjectExists() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(gradeRepository.findBySubjectIdOrderByGradeDateDesc(1L)).thenReturn(List.of());
        when(attendanceRepository.findBySubjectSubjectIdOrderByLessonDateDesc(1L)).thenReturn(List.of());

        Resource resource = reportService.buildSubjectGradesExcel(1L);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        verify(subjectRepository).findById(1L);
        verify(gradeRepository).findBySubjectIdOrderByGradeDateDesc(1L);
    }

    @Test
    @DisplayName("buildSubjectGradesPdf возвращает ресурс при существующем предмете")
    void buildSubjectGradesPdf_ShouldReturnResource_WhenSubjectExists() {
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(gradeRepository.findBySubjectIdOrderByGradeDateDesc(1L)).thenReturn(List.of());
        when(attendanceRepository.findBySubjectSubjectIdOrderByLessonDateDesc(1L)).thenReturn(List.of());

        Resource resource = reportService.buildSubjectGradesPdf(1L);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        verify(subjectRepository).findById(1L);
    }

    @Test
    @DisplayName("buildStudentGradesPdf выбрасывает исключение при отсутствии студента")
    void buildStudentGradesPdf_ShouldThrow_WhenStudentNotFound() {
        when(studentRepository.findByIdWithUserAndGroup(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.buildStudentGradesPdf(2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Студент не найден");

        verify(studentRepository).findByIdWithUserAndGroup(2L);
    }

    @Test
    @DisplayName("buildStudentGradesExcel с фильтром по предмету возвращает ресурс")
    void buildStudentGradesExcel_WithSubjectFilter_ShouldReturnResource() {
        when(studentRepository.findByIdWithUserAndGroup(1L)).thenReturn(Optional.of(student));
        when(gradeRepository.findByStudentIdOrderByDateDescWithSubject(1L)).thenReturn(List.of());
        when(attendanceRepository.findByStudentStudentIdOrderByLessonDateDescWithDetails(1L)).thenReturn(List.of());

        Resource resource = reportService.buildStudentGradesExcel(1L, null, null, 1L);

        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        verify(gradeRepository).findByStudentIdOrderByDateDescWithSubject(1L);
    }
}
