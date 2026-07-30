package com.example.imagehostingservice.exception;

public class InvalidPasswordResetTokenException
        extends RuntimeException {

    public InvalidPasswordResetTokenException(String message) {
        super(message);
    }
}