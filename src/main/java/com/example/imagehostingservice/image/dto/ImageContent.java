package com.example.imagehostingservice.image.dto;

import java.io.InputStream;

public record ImageContent(
        InputStream inputStream,
        String contentType,
        long contentLength,
        String originalFilename
) {
}
