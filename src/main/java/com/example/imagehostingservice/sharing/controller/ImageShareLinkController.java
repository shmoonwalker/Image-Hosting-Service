package com.example.imagehostingservice.sharing.controller;

import com.example.imagehostingservice.sharing.dto.CreateShareLinkRequest;
import com.example.imagehostingservice.sharing.dto.ShareLinkCreatedResponse;
import com.example.imagehostingservice.sharing.dto.ShareLinkListResponse;
import com.example.imagehostingservice.sharing.service.ImageShareLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/images/{imageId}/share-links")
@RequiredArgsConstructor
public class ImageShareLinkController {

    private final ImageShareLinkService shareLinkService;

    @PostMapping
    public ResponseEntity<ShareLinkCreatedResponse> createShareLink(
            @PathVariable UUID imageId,
            @Valid @RequestBody CreateShareLinkRequest request,
            Authentication authentication
    ) {
        ShareLinkCreatedResponse response =
                shareLinkService.createShareLink(
                        authentication.getName(),
                        imageId,
                        request.expiration()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ShareLinkListResponse> getShareLinks(
            @PathVariable UUID imageId,
            Authentication authentication
    ) {
        ShareLinkListResponse response =
                shareLinkService.getImageShareLinks(
                        authentication.getName(),
                        imageId
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{shareLinkId}")
    public ResponseEntity<Void> revokeShareLink(
            @PathVariable UUID imageId,
            @PathVariable UUID shareLinkId,
            Authentication authentication
    ) {
        shareLinkService.revokeShareLink(
                authentication.getName(),
                imageId,
                shareLinkId
        );

        return ResponseEntity.noContent().build();
    }
}
