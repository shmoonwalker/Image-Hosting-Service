package com.example.imagehostingservice.image.service;

import com.example.imagehostingservice.exception.ImageNotFoundException;
import com.example.imagehostingservice.image.dto.ImageContent;
import com.example.imagehostingservice.image.dto.ImageResponse;
import com.example.imagehostingservice.image.model.Image;
import com.example.imagehostingservice.image.repository.ImageRepository;
import com.example.imagehostingservice.storage.service.ObjectStorageService;
import com.example.imagehostingservice.user.model.User;
import com.example.imagehostingservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.example.imagehostingservice.image.validation.ImageFileValidator;
import com.example.imagehostingservice.image.validation.ValidatedImage;

import java.io.InputStream;


@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageFileValidator imageFileValidator;
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
        ValidatedImage validatedImage =
                imageFileValidator.validate(file);

        String originalStorageKey =
                objectStorageService.upload(file);

        Image savedImage = imageRepository.save(
                owner.id(),
                validatedImage.originalFilename(),
                originalStorageKey,
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
}