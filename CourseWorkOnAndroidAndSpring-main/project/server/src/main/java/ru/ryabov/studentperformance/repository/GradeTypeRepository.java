package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.GradeType;

import java.util.List;

@Repository
public interface GradeTypeRepository extends JpaRepository<GradeType, Long> {

    List<GradeType> findAllByOrderByName();
}
