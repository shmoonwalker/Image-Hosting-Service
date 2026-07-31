package com.example.imagehostingservice.sharing.dto;

import com.example.imagehostingservice.image.model.ImageTags;
import com.example.imagehostingservice.image.model.TaggingStatus;

public record SharedImageResponse(
        String originalFilename,
        String contentType,
        Long sizeBytes,
        Integer width,
        Integer height,
        ImageTags aiTags,
        TaggingStatus taggingStatus,
        String contentUrl,
        String thumbnailUrl
) {
}
