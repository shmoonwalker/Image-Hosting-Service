package com.example.imagehostingservice.exception;

public class ShareLinkNotFoundException extends RuntimeException {

    public ShareLinkNotFoundException() {
        super("Sharing link was not found");
    }
}
