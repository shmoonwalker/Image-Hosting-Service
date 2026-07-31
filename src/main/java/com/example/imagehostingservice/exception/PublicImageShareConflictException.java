package com.example.imagehostingservice.exception;

public class PublicImageShareConflictException extends RuntimeException {

    public PublicImageShareConflictException() {
        super("Sharing links can only be created for private images");
    }
}
