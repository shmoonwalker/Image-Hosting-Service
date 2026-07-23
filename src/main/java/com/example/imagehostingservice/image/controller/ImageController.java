package com.example.imagehostingservice.image.controller;

import com.example.imagehostingservice.image.dto.ImagePageResponse;
import com.example.imagehostingservice.image.dto.ImageResponse;
import com.example.imagehostingservice.image.dto.UpdateImageVisibilityRequest;
import com.example.imagehostingservice.image.service.ImageService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.imagehostingservice.image.dto.ImageContent;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;

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

    @GetMapping
    public ResponseEntity<ImagePageResponse> getPublicImages(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(50)
            int size
    ) {
        ImagePageResponse response =
                imageService.getPublicImages(page, size);

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{imageId}")
    public ResponseEntity<ImageResponse> updateImageVisibility(
            @PathVariable Long imageId,
            @Valid @RequestBody
            UpdateImageVisibilityRequest request,
            Authentication authentication
    ) {
        ImageResponse response =
                imageService.updateImageVisibility(
                        authentication.getName(),
                        imageId,
                        request.isPublic()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/mine")
    public ResponseEntity<ImagePageResponse> getMyImages(
            @RequestParam(defaultValue = "0")
            @Min(0)
            int page,

            @RequestParam(defaultValue = "20")
            @Min(1)
            @Max(50)
            int size,

            Authentication authentication
    ) {
        ImagePageResponse response =
                imageService.getMyImages(
                        authentication.getName(),
                        page,
                        size
                );

        return ResponseEntity.ok(response);
    }


    @GetMapping("/{imageId}")
    public ResponseEntity<ImageResponse> getImage(
            @PathVariable Long imageId,
            Authentication authentication
    ) {
        String requesterEmail = authentication == null
                ? null
                : authentication.getName();

        ImageResponse response = imageService.getImage(
                imageId,
                requesterEmail
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{imageId}/thumbnail")
    public ResponseEntity<InputStreamResource> getImageThumbnail(
            @PathVariable Long imageId,
            Authentication authentication
    ) {
        String requesterEmail = authentication == null
                ? null
                : authentication.getName();

        ImageContent content =
                imageService.getImageThumbnail(
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

    @DeleteMapping("/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long imageId,
            Authentication authentication
    ) {
        imageService.deleteImage(
                authentication.getName(),
                imageId
        );

        return ResponseEntity.noContent().build();
    }
}