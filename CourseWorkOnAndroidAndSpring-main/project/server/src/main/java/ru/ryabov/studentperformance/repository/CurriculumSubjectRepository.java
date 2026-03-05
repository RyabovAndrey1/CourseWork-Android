package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.CurriculumSubject;

import java.util.List;

@Repository
public interface CurriculumSubjectRepository extends JpaRepository<CurriculumSubject, Long> {

    List<CurriculumSubject> findByCurriculumId(Integer curriculumId);

    List<CurriculumSubject> findByCurriculumIdAndSemester(Integer curriculumId, Integer semester);

    List<CurriculumSubject> findBySubjectSubjectId(Long subjectId);
}
