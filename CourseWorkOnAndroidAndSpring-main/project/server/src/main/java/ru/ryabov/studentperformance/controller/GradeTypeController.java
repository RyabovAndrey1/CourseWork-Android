package ru.ryabov.studentperformance.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ryabov.studentperformance.dto.common.ApiResponse;
import ru.ryabov.studentperformance.dto.grade.GradeTypeDto;
import ru.ryabov.studentperformance.entity.GradeType;
import ru.ryabov.studentperformance.repository.GradeTypeRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/grade-types")
public class GradeTypeController {

    @Autowired
    private GradeTypeRepository gradeTypeRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEANERY', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<GradeTypeDto>>> getAll() {
        if (gradeTypeRepository.count() == 0) {
            ensureDefaultGradeTypes();
        }
        List<GradeTypeDto> list = gradeTypeRepository.findAllByOrderByName().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    private void ensureDefaultGradeTypes() {
        saveGradeType("Лекция", new BigDecimal("0.2"), "Посещение лекций", "LECTURE", 2);
        saveGradeType("Практика", new BigDecimal("0.3"), "Практические занятия", "PRACTICE", 4);
        saveGradeType("Лабораторная работа", new BigDecimal("0.25"), "Лабораторные работы", "LAB", 4);
        saveGradeType("Контрольная работа", new BigDecimal("0.15"), "Контрольные работы", "CONTROL", 4);
        saveGradeType("Экзамен", new BigDecimal("0.1"), "Итоговый экзамен", "EXAM", 40);
        saveGradeType("Зачёт", new BigDecimal("0.1"), "Итоговый зачёт", "CREDIT", 40);
    }

    private GradeType saveGradeType(String name, BigDecimal weight, String desc, String code, int maxScore) {
        GradeType gt = new GradeType(name, weight, desc);
        gt.setCode(code);
        gt.setMaxScore(maxScore);
        gt.setCreatedAt(LocalDateTime.now());
        return gradeTypeRepository.saveAndFlush(gt);
    }

    private GradeTypeDto toDto(GradeType g) {
        return new GradeTypeDto(
                g.getTypeId(),
                g.getName(),
                g.getWeight(),
                g.getDescription(),
                g.getCode(),
                g.getMaxScore()
        );
    }
}
