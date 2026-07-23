package com.example.imagehostingservice.image.validation;

import com.example.imagehostingservice.exception.InvalidImageException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import java.awt.image.BufferedImage;
import java.util.Set;
import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;

@Component
public class ImageFileValidator {

    private static final long MAXIMUM_FILE_SIZE =
            10L * 1024 * 1024;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png"
            );

    public ValidatedImage validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidImageException(
                    "Image file is required"
            );
        }

        if (file.getSize() > MAXIMUM_FILE_SIZE) {
            throw new InvalidImageException(
                    "Image cannot be larger than 10 MB"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new InvalidImageException(
                    "Only JPEG and PNG images are supported"
            );
        }

        String originalFilename =
                sanitizeFilename(file.getOriginalFilename());

        BufferedImage image = readImage(file);

        return new ValidatedImage(
                originalFilename,
                contentType,
                file.getSize(),
                image.getWidth(),
                image.getHeight()
        );
    }

    private String sanitizeFilename(String originalFilename) {
        if (originalFilename == null ||
                originalFilename.isBlank()) {
            throw new InvalidImageException(
                    "Image filename is required"
            );
        }

        String sanitizedFilename =
                originalFilename.replace('\\', '/');

        int lastSlash = sanitizedFilename.lastIndexOf('/');

        if (lastSlash >= 0) {
            sanitizedFilename =
                    sanitizedFilename.substring(lastSlash + 1);
        }

        if (sanitizedFilename.isBlank()) {
            throw new InvalidImageException(
                    "Image filename is required"
            );
        }

        if (sanitizedFilename.length() > 255) {
            throw new InvalidImageException(
                    "Image filename cannot exceed 255 characters"
            );
        }

        return sanitizedFilename;
    }

    private BufferedImage readImage(MultipartFile file) {
        BufferedImage image;

        try (InputStream inputStream = file.getInputStream()) {
            image = ImageIO.read(inputStream);
        } catch (IOException exception) {
            throw new InvalidImageException(
                    "Could not read the uploaded image",
                    exception
            );
        }

        if (image == null) {
            throw new InvalidImageException(
                    "The uploaded file is not a valid image"
            );
        }

        return image;
    }
}