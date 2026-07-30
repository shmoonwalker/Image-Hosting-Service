package com.example.imagehostingservice.image.dto;

import com.example.imagehostingservice.image.model.ImageTags;
import com.example.imagehostingservice.image.model.TaggingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ImageResponse(
        UUID id,
        String originalFilename,
        String contentType,
        Long sizeBytes,
        Integer width,
        Integer height,
        boolean isPublic,
        ImageTags aiTags,
        TaggingStatus taggingStatus,
        String contentUrl,
        String thumbnailUrl,
        OffsetDateTime createdAt
) {
}