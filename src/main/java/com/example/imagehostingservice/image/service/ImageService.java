package com.example.imagehostingservice.image.service;

import com.example.imagehostingservice.exception.ImageNotFoundException;
import com.example.imagehostingservice.image.dto.ImageContent;
import com.example.imagehostingservice.image.dto.ImagePageResponse;
import com.example.imagehostingservice.image.dto.ImageResponse;
import com.example.imagehostingservice.image.model.Image;
import com.example.imagehostingservice.image.repository.ImageRepository;
import com.example.imagehostingservice.image.thumbnail.ThumbnailGenerator;
import com.example.imagehostingservice.storage.service.ObjectStorageService;
import com.example.imagehostingservice.user.model.User;
import com.example.imagehostingservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.imagehostingservice.image.validation.ImageFileValidator;
import com.example.imagehostingservice.image.validation.ValidatedImage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageFileValidator imageFileValidator;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final ImageRepository imageRepository;
    private final ThumbnailGenerator thumbnailGenerator;

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
        ValidatedImage validatedImage =
                imageFileValidator.validate(file);

        byte[] thumbnailBytes =
                thumbnailGenerator.generate(file);

        String originalStorageKey =
                objectStorageService.upload(file);

        String thumbnailStorageKey =
                objectStorageService.upload(
                        thumbnailBytes,
                        validatedImage.contentType()
                );

        Image savedImage = imageRepository.save(
                owner.id(),
                validatedImage.originalFilename(),
                originalStorageKey,
                thumbnailStorageKey,
                validatedImage.contentType(),
                validatedImage.sizeBytes(),
                validatedImage.width(),
                validatedImage.height()
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

    public ImageContent getImageContent(
            Long imageId,
            String requesterEmail
    ) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(ImageNotFoundException::new);

        if (!image.isPublic() &&
                !isOwner(image, requesterEmail)) {
            throw new ImageNotFoundException();
        }

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
    private boolean isOwner(
            Image image,
            String requesterEmail
    ) {
        if (requesterEmail == null) {
            return false;
        }

        return userRepository.findByEmail(requesterEmail)
                .map(user ->
                        user.id().equals(image.ownerId())
                )
                .orElse(false);
    }

    public ImagePageResponse getPublicImages(int page, int size) {
        int offset = page * size;

        List<ImageResponse> images = imageRepository
                .findAllPublic(size, offset)
                .stream()
                .map(image -> new ImageResponse(
                        image.id(),
                        image.ownerId(),
                        image.originalFilename(),
                        image.contentType(),
                        image.sizeBytes(),
                        image.width(),
                        image.height(),
                        image.isPublic(),
                        image.taggingStatus(),
                        image.createdAt()
                ))
                .toList();

        long totalElements =
                imageRepository.countPublicImages();

        int totalPages = (int) Math.ceil(
                (double) totalElements / size
        );

        return new ImagePageResponse(
                images,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    public ImageResponse updateImageVisibility(
            String ownerEmail,
            Long imageId,
            boolean isPublic
    ) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found"
                        )
                );

        Image updatedImage = imageRepository
                .updateVisibility(
                        imageId,
                        owner.id(),
                        isPublic
                )
                .orElseThrow(ImageNotFoundException::new);

        return new ImageResponse(
                updatedImage.id(),
                updatedImage.ownerId(),
                updatedImage.originalFilename(),
                updatedImage.contentType(),
                updatedImage.sizeBytes(),
                updatedImage.width(),
                updatedImage.height(),
                updatedImage.isPublic(),
                updatedImage.taggingStatus(),
                updatedImage.createdAt()
        );
    }

    public ImageResponse getImage(
            Long imageId,
            String requesterEmail
    ) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(ImageNotFoundException::new);

        if (!image.isPublic() &&
                !isOwner(image, requesterEmail)) {
            throw new ImageNotFoundException();
        }

        return new ImageResponse(
                image.id(),
                image.ownerId(),
                image.originalFilename(),
                image.contentType(),
                image.sizeBytes(),
                image.width(),
                image.height(),
                image.isPublic(),
                image.taggingStatus(),
                image.createdAt()
        );
    }

    public ImagePageResponse getMyImages(
            String ownerEmail,
            int page,
            int size
    ) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found"
                        )
                );

        int offset = page * size;

        List<ImageResponse> images = imageRepository
                .findAllByOwnerId(
                        owner.id(),
                        size,
                        offset
                )
                .stream()
                .map(image -> new ImageResponse(
                        image.id(),
                        image.ownerId(),
                        image.originalFilename(),
                        image.contentType(),
                        image.sizeBytes(),
                        image.width(),
                        image.height(),
                        image.isPublic(),
                        image.taggingStatus(),
                        image.createdAt()
                ))
                .toList();

        long totalElements =
                imageRepository.countByOwnerId(owner.id());

        int totalPages = (int) Math.ceil(
                (double) totalElements / size
        );

        return new ImagePageResponse(
                images,
                page,
                size,
                totalElements,
                totalPages
        );
    }

    public ImageContent getImageThumbnail(
            Long imageId,
            String requesterEmail
    ) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(ImageNotFoundException::new);

        if (!image.isPublic() &&
                !isOwner(image, requesterEmail)) {
            throw new ImageNotFoundException();
        }

        String thumbnailStorageKey =
                image.thumbnailStorageKey();

        if (thumbnailStorageKey == null ||
                thumbnailStorageKey.isBlank()) {
            throw new ImageNotFoundException();
        }

        byte[] thumbnailBytes =
                objectStorageService.downloadBytes(
                        thumbnailStorageKey
                );

        return new ImageContent(
                new ByteArrayInputStream(thumbnailBytes),
                image.contentType(),
                thumbnailBytes.length,
                "thumbnail-" + image.originalFilename()
        );
    }

}