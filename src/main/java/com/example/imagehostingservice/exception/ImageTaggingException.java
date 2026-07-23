package com.example.imagehostingservice.exception;

public class ImageTaggingException extends RuntimeException {

    public ImageTaggingException(String message) {
        super(message);
    }

    public ImageTaggingException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}