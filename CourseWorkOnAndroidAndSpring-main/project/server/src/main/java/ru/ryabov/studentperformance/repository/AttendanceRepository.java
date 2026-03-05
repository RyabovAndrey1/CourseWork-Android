package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.Attendance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentStudentIdOrderByLessonDateDesc(Long studentId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH s.user JOIN FETCH a.subject WHERE a.student.studentId = :studentId ORDER BY a.lessonDate DESC")
    List<Attendance> findByStudentStudentIdOrderByLessonDateDescWithDetails(@Param("studentId") Long studentId);

    List<Attendance> findByStudentStudentIdAndSubjectSubjectIdOrderByLessonDateDesc(Long studentId, Long subjectId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student JOIN FETCH a.subject WHERE a.student.group.groupId = :groupId AND a.subject.subjectId = :subjectId ORDER BY a.lessonDate DESC")
    List<Attendance> findByGroupAndSubjectOrderByDate(@Param("groupId") Long groupId, @Param("subjectId") Long subjectId);

    @Query("SELECT a FROM Attendance a WHERE a.student.studentId = :studentId AND a.subject.subjectId = :subjectId AND a.lessonDate = :date")
    Optional<Attendance> findByStudentAndSubjectAndDate(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.studentId = :studentId AND a.subject.subjectId = :subjectId AND a.present = true")
    long countPresentByStudentAndSubject(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    @Query("SELECT COUNT(a) FROM Attendance a WHERE a.student.studentId = :studentId AND a.subject.subjectId = :subjectId")
    long countTotalByStudentAndSubject(@Param("studentId") Long studentId, @Param("subjectId") Long subjectId);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH s.user LEFT JOIN FETCH s.group LEFT JOIN FETCH a.subject WHERE a.assignment.assignmentId IN :assignmentIds AND a.lessonDate BETWEEN :dateFrom AND :dateTo ORDER BY a.lessonDate DESC")
    List<Attendance> findByAssignmentIdInAndLessonDateBetween(@Param("assignmentIds") List<Long> assignmentIds, @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo);

    @Query("SELECT a FROM Attendance a JOIN FETCH a.student s JOIN FETCH s.user LEFT JOIN FETCH s.group JOIN FETCH a.subject WHERE a.subject.subjectId = :subjectId ORDER BY a.lessonDate DESC")
    List<Attendance> findBySubjectSubjectIdOrderByLessonDateDesc(@Param("subjectId") Long subjectId);
}
