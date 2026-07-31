package com.example.imagehostingservice.sharing.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageShareLink(
        Long id,
        UUID publicId,
        Long imageId,
        String tokenHash,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        OffsetDateTime createdAt
) {
}