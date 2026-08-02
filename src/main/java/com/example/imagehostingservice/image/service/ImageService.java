package com.example.imagehostingservice.image.service;

import com.example.imagehostingservice.exception.ImageNotFoundException;
import com.example.imagehostingservice.image.dto.ImageContent;
import com.example.imagehostingservice.image.dto.ImagePageResponse;
import com.example.imagehostingservice.image.dto.ImageResponse;
import com.example.imagehostingservice.image.model.Image;
import com.example.imagehostingservice.image.model.TaggingStatus;
import com.example.imagehostingservice.image.repository.ImageRepository;
import com.example.imagehostingservice.image.tagging.dispatch.ImageTaggingDispatcher;
import com.example.imagehostingservice.image.tagging.repository.ImageTaggingRepository;
import com.example.imagehostingservice.image.thumbnail.ThumbnailGenerator;
import com.example.imagehostingservice.storage.service.ObjectStorageService;
import com.example.imagehostingservice.user.model.User;
import com.example.imagehostingservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.imagehostingservice.image.validation.ImageFileValidator;
import com.example.imagehostingservice.image.validation.ValidatedImage;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageFileValidator imageFileValidator;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final ImageRepository imageRepository;
    private final ThumbnailGenerator thumbnailGenerator;
    private final ImageTaggingDispatcher imageTaggingDispatcher;
    private final ImageTaggingRepository imageTaggingRepository;

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

        String originalStorageKey = null;
        String thumbnailStorageKey = null;
        Image savedImage;

        try {
            originalStorageKey =
                    objectStorageService.upload(file);

            thumbnailStorageKey =
                    objectStorageService.upload(
                            thumbnailBytes,
                            "image/png"
                    );

            savedImage = imageRepository.save(
                    owner.id(),
                    validatedImage.originalFilename(),
                    originalStorageKey,
                    thumbnailStorageKey,
                    validatedImage.contentType(),
                    validatedImage.sizeBytes(),
                    validatedImage.width(),
                    validatedImage.height()
            );
        } catch (RuntimeException exception) {
            deleteStorageObjectQuietly(
                    thumbnailStorageKey,
                    "thumbnail"
            );
            deleteStorageObjectQuietly(
                    originalStorageKey,
                    "original"
            );

            throw exception;
        }

        log.info(
                "Image uploaded imageId={} userId={} contentType={} sizeBytes={}",
                savedImage.id(),
                savedImage.ownerId(),
                savedImage.contentType(),
                savedImage.sizeBytes()
        );

        Image responseImage = dispatchTagging(savedImage);

        return toResponse(responseImage);
    }

    private Image dispatchTagging(Image savedImage) {
        try {
            imageTaggingDispatcher.dispatch(savedImage.id());
            return savedImage;
        } catch (RuntimeException dispatchException) {
            log.error(
                    "Could not dispatch image tagging imageId={}",
                    savedImage.id(),
                    dispatchException
            );

            try {
                boolean markedFailed =
                        imageTaggingRepository.markPendingFailed(
                                savedImage.id()
                        );

                if (!markedFailed) {
                    log.warn(
                            "Could not mark pending image tagging as failed "
                                    + "imageId={}",
                            savedImage.id()
                    );
                    return savedImage;
                }

                return imageRepository.findById(savedImage.id())
                        .orElse(savedImage);
            } catch (RuntimeException statusException) {
                log.error(
                        "Could not update tagging status after dispatch failure "
                                + "imageId={}",
                        savedImage.id(),
                        statusException
                );
                return savedImage;
            }
        }
    }

    private void deleteStorageObjectQuietly(
            String storageKey,
            String objectType
    ) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }

        try {
            objectStorageService.delete(storageKey);
        } catch (RuntimeException cleanupException) {
            log.error(
                    "Could not clean up uploaded {} object storageKey={}",
                    objectType,
                    storageKey,
                    cleanupException
            );
        }
    }

    public ImageContent getImageContent(
            UUID publicId,
            String requesterEmail
    ) {
        Image image = imageRepository.findByPublicId(publicId)
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

    public ImagePageResponse getPublicImages( String query,
                                              String contentType,
                                              String color,
                                              TaggingStatus taggingStatus,
                                              int page,
                                              int size) {
        int offset = page * size;

        List<ImageResponse> images = imageRepository
                .findAllPublic(
                        query,
                        contentType,
                        color,
                        taggingStatus,
                        size,
                        offset
                )
                .stream()
                .map(this::toResponse)
                .toList();

        long totalElements =
                imageRepository.countPublicImages(
                        query,
                        contentType,
                        color,
                        taggingStatus
                );

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
            UUID publicId,
            boolean isPublic
    ) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found"
                        )
                );

        Image updatedImage = imageRepository.updateVisibilityByPublicId(
                        publicId,
                        owner.id(),
                        isPublic
                )
                .orElseThrow(ImageNotFoundException::new);
        log.info(
                "Image visibility changed imageId={} userId={} public={}",
                updatedImage.id(),
                updatedImage.ownerId(),
                updatedImage.isPublic()
        );

        return toResponse(updatedImage);
    }

    public ImageResponse getImage(
            UUID publicId,
            String requesterEmail
    ) {
        Image image = imageRepository.findByPublicId(publicId)
                .orElseThrow(ImageNotFoundException::new);

        if (!image.isPublic() &&
                !isOwner(image, requesterEmail)) {
            throw new ImageNotFoundException();
        }

        return toResponse(image);
    }

    public ImagePageResponse getMyImages(
            String ownerEmail,
            String query,
            String contentType,
            String color,
            TaggingStatus taggingStatus,
            Boolean isPublic,
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
                        query,
                        contentType,
                        color,
                        taggingStatus,
                        isPublic,
                        size,
                        offset
                )
                .stream()
                .map(this::toResponse)
                .toList();

        long totalElements =
                imageRepository.countByOwnerId(
                        owner.id(),
                        query,
                        contentType,
                        color,
                        taggingStatus,
                        isPublic
                );

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
            UUID publicId,
            String requesterEmail
    ) {
        Image image = imageRepository.findByPublicId(publicId)
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
                "image/png",
                thumbnailBytes.length,
                "thumbnail-" + image.publicId() + ".png"
        );
    }

    public void deleteImage(
            String ownerEmail,
            UUID publicId
    ) {
        User owner = userRepository.findByEmail(ownerEmail)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user was not found"
                        )
                );

        Image image = imageRepository.findByPublicId(publicId)
                .orElseThrow(ImageNotFoundException::new);

        if (!image.ownerId().equals(owner.id())) {
            throw new ImageNotFoundException();
        }

        objectStorageService.delete(
                image.originalStorageKey()
        );

        if (image.thumbnailStorageKey() != null &&
                !image.thumbnailStorageKey().isBlank()) {
            objectStorageService.delete(
                    image.thumbnailStorageKey()
            );
        }

        boolean deleted =
                imageRepository.deleteByPublicIdAndOwnerId(
                        publicId,
                        owner.id()
                );

        if (!deleted) {
            throw new ImageNotFoundException();
        }
        log.info(
                "Image deleted imageId={} userId={}",
                publicId,
                owner.id()
        );
    }
    private ImageResponse toResponse(Image image) {
        String imageUrl =
                "/api/v1/images/" + image.publicId();

        return new ImageResponse(
                image.publicId(),
                image.originalFilename(),
                image.contentType(),
                image.sizeBytes(),
                image.width(),
                image.height(),
                image.isPublic(),
                image.aiTags(),
                image.taggingStatus(),
                imageUrl + "/content",
                imageUrl + "/thumbnail",
                image.createdAt()
        );
    }

}
