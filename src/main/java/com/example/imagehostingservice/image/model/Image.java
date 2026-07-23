package com.example.imagehostingservice.image.model;

import java.time.OffsetDateTime;

public record Image(
        Long id,
        Long ownerId,
        String originalFilename,
        String originalStorageKey,
        String thumbnailStorageKey,
        String contentType,
        Long sizeBytes,
        Integer width,
        Integer height,
        boolean isPublic,
        ImageTags aiTags,
        TaggingStatus taggingStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}