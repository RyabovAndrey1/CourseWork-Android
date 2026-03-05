package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.Teacher;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    @Query("SELECT t FROM Teacher t WHERE t.user.userId = :userId")
    Optional<Teacher> findByUserId(@Param("userId") Long userId);

    List<Teacher> findByDepartmentDepartmentId(Long departmentId);

    @Query("SELECT t FROM Teacher t JOIN FETCH t.user WHERE t.teacherId = :id")
    Optional<Teacher> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT t FROM Teacher t JOIN FETCH t.user LEFT JOIN FETCH t.department WHERE t.teacherId = :id")
    Optional<Teacher> findByIdWithUserAndDepartment(@Param("id") Long id);

    @Query("SELECT t FROM Teacher t JOIN FETCH t.user")
    List<Teacher> findAllWithUser();

    @Query("SELECT t FROM Teacher t JOIN FETCH t.user WHERE t.department.faculty.facultyId = :facultyId")
    List<Teacher> findByFacultyId(@Param("facultyId") Long facultyId);

    @Query("SELECT COUNT(t) FROM Teacher t WHERE t.department.departmentId = :departmentId")
    long countByDepartmentId(@Param("departmentId") Long departmentId);

    @Query("SELECT t FROM Teacher t WHERE t.user.lastName LIKE %:name% OR t.user.firstName LIKE %:name%")
    List<Teacher> searchByName(@Param("name") String name);
}