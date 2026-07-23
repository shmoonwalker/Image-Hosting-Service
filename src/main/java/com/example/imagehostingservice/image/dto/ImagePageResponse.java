package com.example.imagehostingservice.image.dto;

import java.util.List;

public record ImagePageResponse(
        List<ImageResponse> images,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
