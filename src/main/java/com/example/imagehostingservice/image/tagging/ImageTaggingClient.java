package com.example.imagehostingservice.image.tagging;

import com.example.imagehostingservice.image.model.ImageTags;

public interface ImageTaggingClient {

    ImageTags analyze(
            byte[] imageBytes,
            String contentType
    );
}