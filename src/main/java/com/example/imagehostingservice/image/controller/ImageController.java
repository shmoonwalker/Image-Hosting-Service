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
}