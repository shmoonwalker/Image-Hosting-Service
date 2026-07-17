package com.example.imagehostingservice.auth.dto;

import java.time.LocalDateTime;

public record AuthenticatedUserResponse(
        Long id,
        String name,
        String email,
        LocalDateTime createdAt
) {
}
