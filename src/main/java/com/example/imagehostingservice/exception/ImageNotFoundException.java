package com.example.imagehostingservice.exception;

public class ImageNotFoundException extends RuntimeException {

    public ImageNotFoundException() {
        super("Image was not found");
    }
}