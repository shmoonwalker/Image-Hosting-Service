package com.example.imagehostingservice.exception;

public class SharedImageNotFoundException extends RuntimeException {

    public SharedImageNotFoundException() {
        super("Shared image was not found");
    }
}
