package com.example.imagehostingservice.auth.passwordreset.email;

public interface PasswordResetEmailSender {

    void sendPasswordResetLink(
            String recipientEmail,
            String resetToken
    );
}