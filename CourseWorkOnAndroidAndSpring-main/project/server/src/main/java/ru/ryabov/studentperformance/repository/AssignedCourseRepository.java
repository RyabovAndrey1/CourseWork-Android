package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.AssignedCourse;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssignedCourseRepository extends JpaRepository<AssignedCourse, Long> {

    List<AssignedCourse> findByTeacherTeacherId(Long teacherId);

    List<AssignedCourse> findByGroupGroupId(Long groupId);

    List<AssignedCourse> findBySubjectSubjectId(Long subjectId);

    @Query("SELECT ac FROM AssignedCourse ac WHERE ac.teacher.teacherId = :teacherId AND ac.group.groupId = :groupId")
    List<AssignedCourse> findByTeacherAndGroup(@Param("teacherId") Long teacherId, @Param("groupId") Long groupId);

    @Query("SELECT ac FROM AssignedCourse ac JOIN FETCH ac.teacher JOIN FETCH ac.group JOIN FETCH ac.subject WHERE ac.assignmentId = :id")
    Optional<AssignedCourse> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT ac FROM AssignedCourse ac WHERE ac.group.groupId = :groupId AND ac.academicYear = :year AND ac.semester = :semester")
    List<AssignedCourse> findByGroupAndYearAndSemester(@Param("groupId") Long groupId,
                                                        @Param("year") Integer year,
                                                        @Param("semester") Integer semester);

    @Query("SELECT ac FROM AssignedCourse ac JOIN FETCH ac.teacher t JOIN FETCH t.user JOIN FETCH ac.group JOIN FETCH ac.subject ORDER BY ac.academicYear DESC, ac.semester, ac.assignmentId")
    List<AssignedCourse> findAllWithTeacherGroupSubject();

    @Query("SELECT ac FROM AssignedCourse ac WHERE ac.teacher.teacherId = :teacherId AND ac.group.groupId = :groupId AND ac.subject.subjectId = :subjectId")
    Optional<AssignedCourse> findByTeacherIdAndGroupIdAndSubjectId(@Param("teacherId") Long teacherId, @Param("groupId") Long groupId, @Param("subjectId") Long subjectId);

    @Query("SELECT COUNT(ac) > 0 FROM AssignedCourse ac WHERE ac.teacher.teacherId = :teacherId AND ac.group.groupId = :groupId AND ac.subject.subjectId = :subjectId AND ac.academicYear = :year AND ac.semester = :semester")
    boolean existsByTeacherAndGroupAndSubjectAndYearAndSemester(@Param("teacherId") Long teacherId, @Param("groupId") Long groupId, @Param("subjectId") Long subjectId, @Param("year") Integer year, @Param("semester") Integer semester);
}
