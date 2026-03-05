package ru.ryabov.dto;

import lombok.Data;

@Data
public class PostIngredientDto {
    private Long ingredientId;
    private String ingredientName;
    private Double quantityValue;
    private String unit;
}
