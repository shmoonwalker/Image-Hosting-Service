package com.example.imagehostingservice.auth.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuthenticatedUserResponse(
        UUID id,
        String name,
        String email,
        OffsetDateTime createdAt
) {
}
