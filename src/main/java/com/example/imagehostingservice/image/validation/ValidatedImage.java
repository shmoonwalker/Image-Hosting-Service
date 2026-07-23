package com.example.imagehostingservice.image.validation;

public record ValidatedImage(
        String originalFilename,
        String contentType,
        long sizeBytes,
        int width,
        int height) {
}
