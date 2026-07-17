package com.example.imagehostingservice.user.model;

import java.time.LocalDateTime;


public record User(Long id,
                   String name,
                   String email,
                   String passwordHash,
                   LocalDateTime createdAt) {
}
