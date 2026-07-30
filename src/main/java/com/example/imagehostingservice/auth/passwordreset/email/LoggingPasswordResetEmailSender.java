package com.example.imagehostingservice.auth.passwordreset.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prod")
public class LoggingPasswordResetEmailSender
        implements PasswordResetEmailSender {

    private final String resetPasswordUrl;

    public LoggingPasswordResetEmailSender(
            @Value(
                    "${app.frontend.reset-password-url:"
                            + "http://localhost:5173/reset-password}"
            )
            String resetPasswordUrl
    ) {
        this.resetPasswordUrl = resetPasswordUrl;
    }

    @Override
    public void sendPasswordResetLink(
            String recipientEmail,
            String resetToken
    ) {
        String resetLink =
                resetPasswordUrl + "?token=" + resetToken;

        log.warn(
                "Development-only password reset link recipient={} link={}",
                recipientEmail,
                resetLink
        );
    }
}