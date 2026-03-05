package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.FinalGrade;

import java.util.List;
import java.util.Optional;

@Repository
public interface FinalGradeRepository extends JpaRepository<FinalGrade, Long> {

    List<FinalGrade> findByStudentStudentId(Long studentId);

    @Query("SELECT fg FROM FinalGrade fg WHERE fg.student.studentId = :studentId ORDER BY fg.academicYear, fg.semester")
    List<FinalGrade> findByStudentIdOrderByYearAndSemester(@Param("studentId") Long studentId);

    @Query("SELECT fg FROM FinalGrade fg WHERE fg.student.studentId = :studentId AND fg.semester = :semester AND fg.academicYear = :academicYear")
    List<FinalGrade> findByStudentAndSemesterAndYear(@Param("studentId") Long studentId,
                                                      @Param("semester") Integer semester,
                                                      @Param("academicYear") Integer academicYear);

    @Query("SELECT fg FROM FinalGrade fg JOIN FETCH fg.student JOIN FETCH fg.subject WHERE fg.student.studentId = :studentId")
    List<FinalGrade> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    Optional<FinalGrade> findByStudentStudentIdAndSubjectSubjectIdAndSemesterAndAcademicYear(
            Long studentId, Long subjectId, Integer semester, Integer academicYear);

    @Query("SELECT fg FROM FinalGrade fg WHERE fg.student.group.groupId = :groupId AND fg.subject.subjectId = :subjectId")
    List<FinalGrade> findByGroupAndSubject(@Param("groupId") Long groupId, @Param("subjectId") Long subjectId);

    @Query("SELECT fg FROM FinalGrade fg WHERE fg.isAcademicDebt = true")
    List<FinalGrade> findAcademicDebts();
}