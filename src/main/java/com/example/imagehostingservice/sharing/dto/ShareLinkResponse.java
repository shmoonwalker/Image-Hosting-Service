package com.example.imagehostingservice.sharing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ShareLinkResponse(
        UUID id,
        UUID imageId,
        OffsetDateTime expiresAt,
        OffsetDateTime revokedAt,
        boolean active,
        OffsetDateTime createdAt
) {
}
