package com.example.imagehostingservice.sharing.service;

import com.example.imagehostingservice.exception.ImageNotFoundException;
import com.example.imagehostingservice.exception.PublicImageShareConflictException;
import com.example.imagehostingservice.exception.ShareLinkNotFoundException;
import com.example.imagehostingservice.exception.SharedImageNotFoundException;
import com.example.imagehostingservice.image.dto.ImageContent;
import com.example.imagehostingservice.image.model.Image;
import com.example.imagehostingservice.image.repository.ImageRepository;
import com.example.imagehostingservice.sharing.dto.ShareLinkCreatedResponse;
import com.example.imagehostingservice.sharing.dto.ShareLinkListResponse;
import com.example.imagehostingservice.sharing.dto.ShareLinkResponse;
import com.example.imagehostingservice.sharing.dto.SharedImageResponse;
import com.example.imagehostingservice.sharing.model.ImageShareLink;
import com.example.imagehostingservice.sharing.model.ShareExpiration;
import com.example.imagehostingservice.sharing.repository.ImageShareLinkRepository;
import com.example.imagehostingservice.storage.service.ObjectStorageService;
import com.example.imagehostingservice.user.model.User;
import com.example.imagehostingservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageShareLinkService {

    private static final int TOKEN_BYTE_LENGTH = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("[A-Za-z0-9_-]{43}");

    private final UserRepository userRepository;
    private final ImageRepository imageRepository;
    private final ImageShareLinkRepository shareLinkRepository;
    private final ObjectStorageService objectStorageService;

    @Transactional
    public ShareLinkCreatedResponse createShareLink(
            String ownerEmail,
            UUID imagePublicId,
            ShareExpiration expiration
    ) {
        Image image = findOwnedImage(ownerEmail, imagePublicId);

        if (image.isPublic()) {
            throw new PublicImageShareConflictException();
        }

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);
        OffsetDateTime expiresAt = OffsetDateTime
                .now(ZoneOffset.UTC)
                .plus(expiration.duration());

        ImageShareLink shareLink = shareLinkRepository.save(
                image.id(),
                tokenHash,
                expiresAt
        );

        log.info(
                "Sharing link created shareLinkId={} imageId={} expiresAt={}",
                shareLink.publicId(),
                image.publicId(),
                shareLink.expiresAt()
        );

        return new ShareLinkCreatedResponse(
                shareLink.publicId(),
                image.publicId(),
                shareUrl(rawToken),
                shareLink.expiresAt(),
                shareLink.createdAt()
        );
    }

    @Transactional(readOnly = true)
    public ShareLinkListResponse getImageShareLinks(
            String ownerEmail,
            UUID imagePublicId
    ) {
        Image image = findOwnedImage(ownerEmail, imagePublicId);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return new ShareLinkListResponse(
                shareLinkRepository.findAllByImageId(image.id())
                        .stream()
                        .map(shareLink -> toResponse(
                                shareLink,
                                image.publicId(),
                                now
                        ))
                        .toList()
        );
    }

    @Transactional
    public void revokeShareLink(
            String ownerEmail,
            UUID imagePublicId,
            UUID shareLinkPublicId
    ) {
        Image image = findOwnedImage(ownerEmail, imagePublicId);

        boolean revoked = shareLinkRepository
                .revokeByPublicIdAndImageId(
                        shareLinkPublicId,
                        image.id()
                );

        if (!revoked) {
            throw new ShareLinkNotFoundException();
        }

        log.info(
                "Sharing link revoked shareLinkId={} imageId={}",
                shareLinkPublicId,
                image.publicId()
        );
    }

    @Transactional(readOnly = true)
    public SharedImageResponse getSharedImage(String rawToken) {
        Image image = findImageByActiveToken(rawToken);
        String shareUrl = shareUrl(rawToken);

        return new SharedImageResponse(
                image.originalFilename(),
                image.contentType(),
                image.sizeBytes(),
                image.width(),
                image.height(),
                image.aiTags(),
                image.taggingStatus(),
                shareUrl + "/content",
                shareUrl + "/thumbnail"
        );
    }

    @Transactional(readOnly = true)
    public ImageContent getSharedImageContent(String rawToken) {
        Image image = findImageByActiveToken(rawToken);
        InputStream inputStream = objectStorageService.download(
                image.originalStorageKey()
        );

        return new ImageContent(
                inputStream,
                image.contentType(),
                image.sizeBytes(),
                image.originalFilename()
        );
    }

    @Transactional(readOnly = true)
    public ImageContent getSharedImageThumbnail(String rawToken) {
        Image image = findImageByActiveToken(rawToken);
        String thumbnailStorageKey = image.thumbnailStorageKey();

        if (thumbnailStorageKey == null ||
                thumbnailStorageKey.isBlank()) {
            throw new SharedImageNotFoundException();
        }

        byte[] thumbnailBytes = objectStorageService.downloadBytes(
                thumbnailStorageKey
        );

        return new ImageContent(
                new ByteArrayInputStream(thumbnailBytes),
                "image/png",
                thumbnailBytes.length,
                "thumbnail-" + image.publicId() + ".png"
        );
    }

    private Image findOwnedImage(
            String ownerEmail,
            UUID imagePublicId
    ) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found"
                        )
                );

        Image image = imageRepository.findByPublicId(imagePublicId)
                .orElseThrow(ImageNotFoundException::new);

        if (!image.ownerId().equals(owner.id())) {
            throw new ImageNotFoundException();
        }

        return image;
    }

    private Image findImageByActiveToken(String rawToken) {
        if (rawToken == null ||
                !TOKEN_PATTERN.matcher(rawToken).matches()) {
            throw new SharedImageNotFoundException();
        }

        ImageShareLink shareLink = shareLinkRepository
                .findActiveByTokenHash(hashToken(rawToken))
                .orElseThrow(SharedImageNotFoundException::new);

        Image image = imageRepository.findById(shareLink.imageId())
                .orElseThrow(SharedImageNotFoundException::new);

        if (image.isPublic()) {
            throw new SharedImageNotFoundException();
        }

        return image;
    }

    private ShareLinkResponse toResponse(
            ImageShareLink shareLink,
            UUID imagePublicId,
            OffsetDateTime now
    ) {
        boolean active = shareLink.revokedAt() == null &&
                shareLink.expiresAt().isAfter(now);

        return new ShareLinkResponse(
                shareLink.publicId(),
                imagePublicId,
                shareLink.expiresAt(),
                shareLink.revokedAt(),
                active,
                shareLink.createdAt()
        );
    }

    private String generateToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTE_LENGTH];
        SECURE_RANDOM.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            return HexFormat.of().formatHex(
                    digest.digest(
                            rawToken.getBytes(
                                    java.nio.charset.StandardCharsets.UTF_8
                            )
                    )
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 hashing is unavailable",
                    exception
            );
        }
    }

    private String shareUrl(String rawToken) {
        return "/api/v1/shares/" + rawToken;
    }
}
