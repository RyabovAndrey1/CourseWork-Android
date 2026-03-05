package ru.ryabov.mapper;

import ru.ryabov.dto.RecipeStepDto;
import ru.ryabov.model.RecipeStep;
import ru.ryabov.util.UrlHelper;

public final class StepMapper {
    private StepMapper() {}

    public static RecipeStepDto toDto(RecipeStep s) {
        if (s == null) return null;
        RecipeStepDto dto = new RecipeStepDto();
        dto.setOrder(s.getOrder());
        dto.setDescription(s.getDescription());
        dto.setImageUrl(UrlHelper.toAbsolute(s.getImageUrl()));
        return dto;
    }
}
