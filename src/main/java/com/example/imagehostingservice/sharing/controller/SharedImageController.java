package com.example.imagehostingservice.sharing.controller;

import com.example.imagehostingservice.image.dto.ImageContent;
import com.example.imagehostingservice.sharing.dto.SharedImageResponse;
import com.example.imagehostingservice.sharing.service.ImageShareLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/shares")
@RequiredArgsConstructor
public class SharedImageController {

    private final ImageShareLinkService shareLinkService;

    @GetMapping("/{token}")
    public ResponseEntity<SharedImageResponse> getSharedImage(
            @PathVariable String token
    ) {
        return ResponseEntity.ok(
                shareLinkService.getSharedImage(token)
        );
    }

    @GetMapping("/{token}/content")
    public ResponseEntity<InputStreamResource> getSharedImageContent(
            @PathVariable String token
    ) {
        return imageContentResponse(
                shareLinkService.getSharedImageContent(token)
        );
    }

    @GetMapping("/{token}/thumbnail")
    public ResponseEntity<InputStreamResource> getSharedImageThumbnail(
            @PathVariable String token
    ) {
        return imageContentResponse(
                shareLinkService.getSharedImageThumbnail(token)
        );
    }

    private ResponseEntity<InputStreamResource> imageContentResponse(
            ImageContent content
    ) {
        ContentDisposition disposition = ContentDisposition.inline()
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
