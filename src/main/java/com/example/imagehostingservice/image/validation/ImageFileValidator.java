package com.example.imagehostingservice.image.validation;

import com.example.imagehostingservice.exception.InvalidImageException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Set;

@Component
public class ImageFileValidator {

    private static final long MAXIMUM_FILE_SIZE =
            10L * 1024 * 1024;

    private static final int MAXIMUM_WIDTH = 10_000;

    private static final int MAXIMUM_HEIGHT = 10_000;

    private static final long MAXIMUM_PIXELS = 25_000_000L;

    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
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
                    "Only JPEG, PNG, and WebP images are supported"
            );
        }

        String originalFilename =
                sanitizeFilename(file.getOriginalFilename());

        ImageDimensions dimensions = inspectImage(file);

        return new ValidatedImage(
                originalFilename,
                contentType,
                file.getSize(),
                dimensions.width(),
                dimensions.height()
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

    private ImageDimensions inspectImage(MultipartFile file) {
        try (
                InputStream inputStream = file.getInputStream();
                ImageInputStream imageInputStream =
                        ImageIO.createImageInputStream(inputStream)
        ) {
            if (imageInputStream == null) {
                throw invalidImage();
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInputStream);

            if (!readers.hasNext()) {
                throw invalidImage();
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInputStream, false, true);

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                validateDimensions(width, height);

                BufferedImage image = reader.read(0);

                if (image == null) {
                    throw invalidImage();
                }

                validateDimensions(
                        image.getWidth(),
                        image.getHeight()
                );

                return new ImageDimensions(
                        image.getWidth(),
                        image.getHeight()
                );
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new InvalidImageException(
                    "Could not read the uploaded image",
                    exception
            );
        }
    }

    private void validateDimensions(int width, int height) {
        long pixelCount = (long) width * height;

        if (width <= 0 ||
                height <= 0 ||
                width > MAXIMUM_WIDTH ||
                height > MAXIMUM_HEIGHT ||
                pixelCount > MAXIMUM_PIXELS) {
            throw new InvalidImageException(
                    "Image dimensions are too large"
            );
        }
    }

    private InvalidImageException invalidImage() {
        return new InvalidImageException(
                "The uploaded file is not a valid image"
        );
    }

    private record ImageDimensions(
            int width,
            int height
    ) {
    }
}
