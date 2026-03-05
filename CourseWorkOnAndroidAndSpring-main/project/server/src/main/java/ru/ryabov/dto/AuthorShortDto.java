package ru.ryabov.dto;

import lombok.Data;

@Data
public class AuthorShortDto {
    private Long id;
    private String displayName;
    private String avatarUrl;
    private boolean isSubscribed;
}
