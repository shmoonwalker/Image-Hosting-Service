package com.example.imagehostingservice.sharing.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ShareLinkCreatedResponse(
        UUID id,
        UUID imageId,
        String shareUrl,
        OffsetDateTime expiresAt,
        OffsetDateTime createdAt
) {
}
