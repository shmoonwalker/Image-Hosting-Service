package com.example.imagehostingservice.auth.passwordreset.email;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import com.resend.services.emails.model.Template;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prod")
public class ResendPasswordResetEmailSender
        implements PasswordResetEmailSender {

    private static final int EXPIRY_MINUTES = 30;

    private final Resend resend;
    private final String fromEmail;
    private final String passwordResetTemplate;
    private final String resetPasswordUrl;

    public ResendPasswordResetEmailSender(
            @Value("${resend.api-key}") String apiKey,
            @Value("${resend.from-email}") String fromEmail,
            @Value("${resend.password-reset-template}")
            String passwordResetTemplate,
            @Value("${app.frontend.reset-password-url}")
            String resetPasswordUrl
    ) {
        this.resend = new Resend(apiKey);
        this.fromEmail = fromEmail;
        this.passwordResetTemplate = passwordResetTemplate;
        this.resetPasswordUrl = resetPasswordUrl;
    }

    @Override
    public void sendPasswordResetLink(
            String recipientEmail,
            String resetToken
    ) {
        String resetLink = resetPasswordUrl + "?token=" + resetToken;

        Template template = Template.builder()
                .id(passwordResetTemplate)
                .addVariable("RESET_URL", resetLink)
                .addVariable("EXPIRY_MINUTES", EXPIRY_MINUTES)
                .build();

        CreateEmailOptions email = CreateEmailOptions.builder()
                .from(fromEmail)
                .to(recipientEmail)
                .template(template)
                .build();

        try {
            CreateEmailResponse response = resend.emails().send(email);

            log.info(
                    "Password reset email accepted by Resend emailId={}",
                    response.getId()
            );
        } catch (ResendException exception) {
            log.error(
                    "Resend rejected password reset email status={} error={}",
                    exception.getStatusCode(),
                    exception.getErrorName(),
                    exception
            );

            throw new IllegalStateException(
                    "Could not send password reset email",
                    exception
            );
        }
    }
}