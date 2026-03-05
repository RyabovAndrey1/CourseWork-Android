package ru.ryabov.dto;

import jakarta.validation.constraints.NotBlank;

public record CommentUpdateDto(
        @NotBlank String content
) {}
