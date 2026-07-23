package com.example.imagehostingservice.image.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateImageVisibilityRequest(
        @NotNull Boolean isPublic
) {
}