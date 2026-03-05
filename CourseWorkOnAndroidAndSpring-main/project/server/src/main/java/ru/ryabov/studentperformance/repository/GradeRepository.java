package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.Grade;
import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.entity.Subject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


@Repository
public interface GradeRepository extends JpaRepository<Grade, Long> {

    List<Grade> findByStudent(Student student);

    List<Grade> findByStudentStudentId(Long studentId);

    List<Grade> findBySubject(Subject subject);

    List<Grade> findBySubjectSubjectId(Long subjectId);

    @Query("SELECT g FROM Grade g JOIN FETCH g.student s JOIN FETCH s.user LEFT JOIN FETCH s.group JOIN FETCH g.subject LEFT JOIN FETCH g.gradeType WHERE g.subject.subjectId = :subjectId ORDER BY g.gradeDate DESC")
    List<Grade> findBySubjectIdOrderByGradeDateDesc(@Param("subjectId") Long subjectId);

    List<Grade> findByStudentAndSubject(Student student, Subject subject);

    @Query("SELECT g FROM Grade g WHERE g.student.studentId = :studentId ORDER BY g.gradeDate DESC")
    List<Grade> findByStudentIdOrderByDateDesc(@Param("studentId") Long studentId);

    @Query("SELECT g FROM Grade g JOIN FETCH g.subject LEFT JOIN FETCH g.gradeType WHERE g.student.studentId = :studentId ORDER BY g.gradeDate DESC")
    List<Grade> findByStudentIdOrderByDateDescWithSubject(@Param("studentId") Long studentId);

    @Query("SELECT g FROM Grade g WHERE g.student.studentId = :studentId AND g.subject.subjectId = :subjectId ORDER BY g.gradeDate DESC")
    List<Grade> findByStudentAndSubjectId(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    @Query("SELECT g FROM Grade g WHERE g.student.group.groupId = :groupId AND g.subject.subjectId = :subjectId ORDER BY g.gradeDate DESC")
    List<Grade> findByGroupAndSubject(@Param("groupId") Long groupId, @Param("subjectId") Long subjectId);

    @Query("SELECT g FROM Grade g JOIN FETCH g.student s JOIN FETCH s.user JOIN FETCH g.subject WHERE g.student.group.groupId = :groupId ORDER BY g.student.studentId, g.gradeDate DESC")
    List<Grade> findByStudentGroupGroupIdOrderByGradeDateDesc(@Param("groupId") Long groupId);

    @Query("SELECT AVG(g.gradeValue) FROM Grade g WHERE g.student.studentId = :studentId")
    BigDecimal findAverageByStudentId(@Param("studentId") Long studentId);

    @Query("SELECT AVG(g.gradeValue) FROM Grade g WHERE g.student.studentId = :studentId AND g.subject.subjectId = :subjectId")
    BigDecimal findAverageByStudentAndSubject(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    @Query("SELECT g FROM Grade g WHERE g.student.studentId = :studentId AND g.semester = :semester AND g.academicYear = :academicYear")
    List<Grade> findByStudentAndSemesterAndYear(@Param("studentId") Long studentId,
                                                  @Param("semester") Integer semester,
                                                  @Param("academicYear") Integer academicYear);

    @Query("SELECT g FROM Grade g JOIN FETCH g.student JOIN FETCH g.subject LEFT JOIN FETCH g.gradeType WHERE g.gradeId = :id")
    Grade findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT g FROM Grade g JOIN FETCH g.student s JOIN FETCH s.user LEFT JOIN FETCH s.group LEFT JOIN FETCH g.gradeType LEFT JOIN FETCH g.subject WHERE g.assignment.assignmentId IN :assignmentIds AND g.gradeDate BETWEEN :dateFrom AND :dateTo ORDER BY g.gradeDate DESC")
    List<Grade> findByAssignmentIdInAndGradeDateBetween(@Param("assignmentIds") List<Long> assignmentIds, @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo);

    @Query("SELECT g FROM Grade g WHERE g.assignment.assignmentId = :assignmentId AND g.student.studentId = :studentId AND g.gradeDate = :gradeDate AND g.gradeType.typeId = :gradeTypeId")
    java.util.Optional<Grade> findByAssignmentAndStudentAndDateAndGradeType(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId, @Param("gradeDate") LocalDate gradeDate, @Param("gradeTypeId") Long gradeTypeId);

    void deleteByStudentStudentId(Long studentId);
}