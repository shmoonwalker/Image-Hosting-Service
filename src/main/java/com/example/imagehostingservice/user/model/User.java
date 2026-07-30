package com.example.imagehostingservice.user.model;

import java.time.OffsetDateTime;
import java.util.UUID;


public record User(Long id,
                   UUID publicId,
                   String name,
                   String email,
                   String passwordHash,
                   OffsetDateTime createdAt) {
}
