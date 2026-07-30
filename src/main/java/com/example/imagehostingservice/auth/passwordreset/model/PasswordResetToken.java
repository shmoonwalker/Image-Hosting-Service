package com.example.imagehostingservice.auth.passwordreset.model;

import java.time.OffsetDateTime;

public record PasswordResetToken(
        Long id,
        Long userId,
        String tokenHash,
        OffsetDateTime expiresAt,
        OffsetDateTime usedAt,
        OffsetDateTime createdAt
) {
}