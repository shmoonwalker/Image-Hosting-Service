package com.example.imagehostingservice.image.service;

import com.example.imagehostingservice.image.dto.ImageResponse;
import com.example.imagehostingservice.image.model.Image;
import com.example.imagehostingservice.image.repository.ImageRepository;
import com.example.imagehostingservice.storage.service.ObjectStorageService;
import com.example.imagehostingservice.user.model.User;
import com.example.imagehostingservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ImageService {

    private static final long MAXIMUM_FILE_SIZE = 10L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png"
    );

    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final ImageRepository imageRepository;

    public ImageResponse uploadImage(
            String ownerEmail,
            MultipartFile file
    ) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found"
                        )
                );

        if (file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Image file is required"
            );
        }

        if (file.getSize() > MAXIMUM_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "Image cannot be larger than 10 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Only JPEG and PNG images are supported"
            );
        }

        String originalFilename = file.getOriginalFilename();

        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException(
                    "Image filename is required"
            );
        }

        originalFilename = originalFilename.replace('\\', '/');

        int lastSlash = originalFilename.lastIndexOf('/');

        if (lastSlash >= 0) {
            originalFilename =
                    originalFilename.substring(lastSlash + 1);
        }

        if (originalFilename.isBlank()) {
            throw new IllegalArgumentException(
                    "Image filename is required"
            );
        }

        if (originalFilename.length() > 255) {
            throw new IllegalArgumentException(
                    "Image filename cannot exceed 255 characters"
            );
        }

        BufferedImage bufferedImage;

        try (InputStream inputStream = file.getInputStream()) {
            bufferedImage = ImageIO.read(inputStream);
        } catch (IOException exception) {
            throw new IllegalArgumentException(
                    "Could not read the uploaded image",
                    exception
            );
        }

        if (bufferedImage == null) {
            throw new IllegalArgumentException(
                    "The uploaded file is not a valid image"
            );
        }

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        String originalStorageKey =
                objectStorageService.upload(file);

        Image savedImage = imageRepository.save(
                owner.id(),
                originalFilename,
                originalStorageKey,
                contentType,
                file.getSize(),
                width,
                height
        );

        return new ImageResponse(
                savedImage.id(),
                savedImage.ownerId(),
                savedImage.originalFilename(),
                savedImage.contentType(),
                savedImage.sizeBytes(),
                savedImage.width(),
                savedImage.height(),
                savedImage.isPublic(),
                savedImage.taggingStatus(),
                savedImage.createdAt()
        );
    }
}