package ru.ryabov.studentperformance.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.ryabov.studentperformance.entity.ReportRecord;

import java.util.List;

@Repository
public interface ReportRecordRepository extends JpaRepository<ReportRecord, Long> {

    List<ReportRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
}
