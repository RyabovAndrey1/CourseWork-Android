package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.Subject;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    List<Subject> findByControlType(Subject.ControlType controlType);

    @Query("SELECT s FROM Subject s WHERE s.name LIKE %:name%")
    List<Subject> searchByName(@Param("name") String name);

    @Query("SELECT s FROM Subject s WHERE s.code = :code")
    Subject findByCode(@Param("code") String code);

    @Query("SELECT s FROM Subject s ORDER BY s.name")
    List<Subject> findAllOrderByName();

    @Query("SELECT s FROM Subject s WHERE s.controlType = :controlType ORDER BY s.name")
    List<Subject> findByControlTypeOrderByName(@Param("controlType") Subject.ControlType controlType);
}