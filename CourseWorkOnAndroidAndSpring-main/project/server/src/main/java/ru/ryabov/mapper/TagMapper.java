package ru.ryabov.mapper;

import ru.ryabov.dto.TagDto;
import ru.ryabov.model.Tag;

public final class TagMapper {
    private TagMapper() {}

    public static TagDto toDto(Tag t) {
        if (t == null) {
            return null;
        }
        TagDto dto = new TagDto();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setColor(t.getColor());
        return dto;
    }
}
