package ru.ryabov.mapper;

import ru.ryabov.dto.PostIngredientDto;
import ru.ryabov.dto.IngredientDto;
import ru.ryabov.model.Ingredient;
import ru.ryabov.model.PostIngredient;

public final class IngredientMapper {
    private IngredientMapper() {}

    public static PostIngredientDto toDto(PostIngredient pi) {
        if (pi == null) return null;
        PostIngredientDto dto = new PostIngredientDto();
        dto.setIngredientId(pi.getIngredient().getId());
        dto.setIngredientName(pi.getIngredient().getName());
        dto.setQuantityValue(pi.getQuantityValue());
        dto.setUnit(pi.getUnit());
        return dto;
    }

    public static IngredientDto toDto(Ingredient ingredient) {
        if (ingredient == null) return null;
        IngredientDto dto = new IngredientDto();
        dto.setId(ingredient.getId());
        dto.setName(ingredient.getName());
        return dto;
    }
}
