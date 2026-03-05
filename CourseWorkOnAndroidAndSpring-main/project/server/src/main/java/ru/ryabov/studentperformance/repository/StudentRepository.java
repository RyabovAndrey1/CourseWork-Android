package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.Student;
import ru.ryabov.studentperformance.entity.StudyGroup;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query("SELECT s FROM Student s WHERE s.user.userId = :userId")
    Optional<Student> findByUserId(@Param("userId") Long userId);

    Optional<Student> findByRecordBookNumber(String recordBookNumber);

    List<Student> findByGroup(StudyGroup group);

    List<Student> findByGroupGroupId(Long groupId);

    @Query("SELECT s FROM Student s WHERE s.group.faculty.facultyId = :facultyId")
    List<Student> findByFacultyId(@Param("facultyId") Long facultyId);

    @Query("SELECT s FROM Student s JOIN FETCH s.user JOIN FETCH s.group WHERE s.studentId = :id")
    Optional<Student> findByIdWithUserAndGroup(@Param("id") Long id);

    @Query("SELECT s FROM Student s JOIN FETCH s.user WHERE s.group.groupId = :groupId")
    List<Student> findByGroupIdWithUser(@Param("groupId") Long groupId);

    @Query("SELECT COUNT(s) FROM Student s WHERE s.group.groupId = :groupId")
    long countByGroupId(@Param("groupId") Long groupId);

    @Query("SELECT s FROM Student s WHERE s.user.lastName LIKE %:name% OR s.user.firstName LIKE %:name%")
    List<Student> searchByName(@Param("name") String name);
}