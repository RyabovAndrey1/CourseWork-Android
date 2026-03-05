package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.Department;

import java.util.List;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findByFacultyFacultyId(Long facultyId);

    List<Department> findByFacultyFacultyIdOrderByName(Long facultyId);

    List<Department> findAllByOrderByName();
}
