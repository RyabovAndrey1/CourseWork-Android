package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.StudyGroup;
import ru.ryabov.studentperformance.entity.Faculty;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<StudyGroup, Long> {

    List<StudyGroup> findByFaculty(Faculty faculty);

    List<StudyGroup> findByFacultyFacultyId(Long facultyId);

    List<StudyGroup> findByAdmissionYear(Integer admissionYear);

    @Query("SELECT g FROM StudyGroup g LEFT JOIN FETCH g.faculty WHERE g.groupId = :id")
    Optional<StudyGroup> findByIdWithFaculty(@Param("id") Long id);

    @Query("SELECT g FROM StudyGroup g LEFT JOIN FETCH g.faculty")
    List<StudyGroup> findAllWithFaculty();

    @Query("SELECT DISTINCT g FROM StudyGroup g LEFT JOIN FETCH g.faculty WHERE g.groupId IN :ids ORDER BY g.name")
    List<StudyGroup> findByIdInWithFaculty(@Param("ids") List<Long> ids);

    @Query("SELECT g FROM StudyGroup g WHERE g.faculty.facultyId = :facultyId ORDER BY g.name")
    List<StudyGroup> findByFacultyIdOrderByName(@Param("facultyId") Long facultyId);

    @Query("SELECT g FROM StudyGroup g WHERE g.admissionYear = :year ORDER BY g.name")
    List<StudyGroup> findByAdmissionYearOrderByName(@Param("year") Integer year);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.group.groupId = :groupId")
    long countStudentsByGroupId(@Param("groupId") Long groupId);
}