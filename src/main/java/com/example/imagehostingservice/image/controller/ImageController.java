package com.example.imagehostingservice.image.controller;

import com.example.imagehostingservice.image.dto.ImageResponse;
import com.example.imagehostingservice.image.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.example.imagehostingservice.image.dto.ImageContent;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImageResponse> uploadImage(
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        ImageResponse response = imageService.uploadImage(
                authentication.getName(),
                file
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{imageId}/content")
    public ResponseEntity<InputStreamResource> getImageContent(
            @PathVariable Long imageId,
            Authentication authentication
    ) {
        String requesterEmail = authentication == null
                ? null
                : authentication.getName();

        ImageContent content = imageService.getImageContent(
                imageId,
                requesterEmail
        );

        ContentDisposition disposition =
                ContentDisposition.inline()
                        .filename(
                                content.originalFilename(),
                                StandardCharsets.UTF_8
                        )
                        .build();

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                content.contentType()
                        )
                )
                .contentLength(content.contentLength())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .header(
                        "X-Content-Type-Options",
                        "nosniff"
                )
                .body(
                        new InputStreamResource(
                                content.inputStream()
                        )
                );
    }
}