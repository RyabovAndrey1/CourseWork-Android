package ru.ryabov.studentperformance.dto.grade;

import java.math.BigDecimal;

public class GradeTypeDto {
    private Long typeId;
    private String name;
    private BigDecimal weight;
    private String description;
    private String code;
    private Integer maxScore;

    public GradeTypeDto() {}
    public GradeTypeDto(Long typeId, String name, BigDecimal weight, String description, String code, Integer maxScore) {
        this.typeId = typeId;
        this.name = name;
        this.weight = weight;
        this.description = description;
        this.code = code;
        this.maxScore = maxScore;
    }
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
}
