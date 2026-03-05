package ru.ryabov.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdatePostStatusRequest(
        @NotBlank String status
) {
}
