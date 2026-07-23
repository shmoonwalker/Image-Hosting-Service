package com.example.imagehostingservice.image.dto;

import com.example.imagehostingservice.image.model.TaggingStatus;

import java.time.OffsetDateTime;

public record ImageResponse(Long id,
                            Long ownerId,
                            String originalFilename,
                            String contentType,
                            Long sizeBytes,
                            Integer width,
                            Integer height,
                            boolean isPublic,
                            TaggingStatus taggingStatus,
                            OffsetDateTime createdAt) {
}
