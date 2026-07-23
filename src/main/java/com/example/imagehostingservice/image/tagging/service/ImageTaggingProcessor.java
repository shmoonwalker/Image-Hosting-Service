package com.example.imagehostingservice.image.tagging.service;

import com.example.imagehostingservice.image.tagging.ImageTaggingClient;
import com.example.imagehostingservice.exception.ImageTaggingException;
import com.example.imagehostingservice.image.model.Image;
import com.example.imagehostingservice.image.model.ImageTags;
import com.example.imagehostingservice.image.repository.ImageRepository;
import com.example.imagehostingservice.image.tagging.repository.ImageTaggingRepository;
import com.example.imagehostingservice.storage.service.ObjectStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageTaggingProcessor {

    private final ImageRepository imageRepository;
    private final ImageTaggingRepository taggingRepository;
    private final ObjectStorageService objectStorageService;
    private final ImageTaggingClient taggingClient;

    public void process(Long imageId) {
        boolean claimed =
                taggingRepository.markProcessing(imageId);

        if (!claimed) {
            log.debug(
                    "Skipping image {} because it is not pending",
                    imageId
            );
            return;
        }

        try {
            Image image = imageRepository.findById(imageId)
                    .orElseThrow(() ->
                            new ImageTaggingException(
                                    "Image was not found"
                            )
                    );

            byte[] imageBytes =
                    objectStorageService.downloadBytes(
                            image.originalStorageKey()
                    );

            ImageTags imageTags = taggingClient.analyze(
                    imageBytes,
                    image.contentType()
            );

            boolean completed =
                    taggingRepository.markCompleted(
                            imageId,
                            imageTags
                    );

            if (!completed) {
                throw new ImageTaggingException(
                        "Could not complete tagging for image "
                                + imageId
                );
            }
        } catch (RuntimeException exception) {
            taggingRepository.markFailed(imageId);

            throw new ImageTaggingException(
                    "Image tagging failed for image "
                            + imageId,
                    exception
            );
        }
    }
}