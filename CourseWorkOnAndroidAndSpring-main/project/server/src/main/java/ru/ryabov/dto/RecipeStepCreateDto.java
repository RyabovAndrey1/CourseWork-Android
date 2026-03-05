package ru.ryabov.dto;

import lombok.Data;

@Data
public class RecipeStepCreateDto {
    private int order;
    private String description;
    private String imageUrl;
}
