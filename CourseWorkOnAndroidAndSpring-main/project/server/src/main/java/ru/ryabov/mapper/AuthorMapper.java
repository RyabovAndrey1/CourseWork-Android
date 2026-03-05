package ru.ryabov.mapper;

import ru.ryabov.dto.AuthorShortDto;
import ru.ryabov.model.User;
import ru.ryabov.util.UrlHelper;

public final class AuthorMapper {
    private AuthorMapper() {}

    public static AuthorShortDto toDto(User u) {
        if (u == null) {
            return null;
        }
        AuthorShortDto dto = new AuthorShortDto();
        dto.setId(u.getId());
        dto.setDisplayName(u.getDisplayName());
        dto.setAvatarUrl(UrlHelper.toAbsolute(u.getAvatarUrl()));
        dto.setSubscribed(false);
        return dto;
    }
}
