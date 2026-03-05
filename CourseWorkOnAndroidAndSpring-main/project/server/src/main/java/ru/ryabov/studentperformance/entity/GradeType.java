package ru.ryabov.studentperformance.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "grade_types")
public class GradeType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long typeId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "weight", precision = 3, scale = 2)
    private BigDecimal weight;

    @Column(name = "description", length = 255)
    private String description;

    /** Код типа для системы 60+40: LECTURE, LAB, PRACTICE, EXAM, CREDIT, CONTROL */
    @Column(name = "code", length = 20)
    private String code;

    /** Макс. баллов за одну запись (2 — лекция, 4 — лаба/практика, 40 — экзамен/зачёт) */
    @Column(name = "max_score")
    private Integer maxScore;

    /** В таблице grade_types столбца created_at нет (только type_id, name, weight, description, code, max_score). */
    @Transient
    private java.time.LocalDateTime createdAt;

    @OneToMany(mappedBy = "gradeType")
    private List<Grade> grades = new ArrayList<>();

    public GradeType() {
    }

    public GradeType(String name, BigDecimal weight, String description) {
        this.name = name;
        this.weight = weight;
        this.description = description;
    }

    // Getters and Setters
    public Long getTypeId() { return typeId; }
    public void setTypeId(Long typeId) { this.typeId = typeId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Integer getMaxScore() { return maxScore; }
    public void setMaxScore(Integer maxScore) { this.maxScore = maxScore; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Grade> getGrades() { return grades; }
    public void setGrades(List<Grade> grades) { this.grades = grades; }
}
