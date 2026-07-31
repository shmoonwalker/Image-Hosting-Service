package com.example.imagehostingservice.auth.passwordreset.service;

import com.example.imagehostingservice.auth.passwordreset.dto.PasswordResetConfirmRequest;
import com.example.imagehostingservice.auth.passwordreset.dto.PasswordResetRequest;
import com.example.imagehostingservice.auth.passwordreset.email.PasswordResetEmailSender;
import com.example.imagehostingservice.auth.passwordreset.model.PasswordResetToken;
import com.example.imagehostingservice.auth.passwordreset.repository.PasswordResetTokenRepository;
import com.example.imagehostingservice.exception.InvalidPasswordResetTokenException;
import com.example.imagehostingservice.user.model.User;
import com.example.imagehostingservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final Duration TOKEN_VALIDITY =
            Duration.ofMinutes(30);

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetEmailSender emailSender;
    private final FindByIndexNameSessionRepository<? extends Session>
            sessionRepository;

    @Transactional
    public void requestPasswordReset(
            PasswordResetRequest request
    ) {
        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        String resetToken = generateToken();
        String tokenHash = hashToken(resetToken);

        User user = userRepository.findByEmail(email)
                .orElse(null);

        if (user == null) {
            log.info("Password reset requested");
            return;
        }

        tokenRepository.invalidateUnusedByUserId(user.id());

        tokenRepository.save(
                user.id(),
                tokenHash,
                OffsetDateTime.now(ZoneOffset.UTC)
                        .plus(TOKEN_VALIDITY)
        );

        emailSender.sendPasswordResetLink(
                user.email(),
                resetToken
        );

        log.info(
                "Password reset requested userId={}",
                user.id()
        );
    }

    @Transactional
    public void confirmPasswordReset(
            PasswordResetConfirmRequest request
    ) {
        String tokenHash = hashToken(request.token());

        PasswordResetToken resetToken = tokenRepository
                .findValidByTokenHash(tokenHash)
                .orElseThrow(
                        PasswordResetService::invalidToken
                );

        User user = userRepository.findById(
                        resetToken.userId()
                )
                .orElseThrow(
                        PasswordResetService::invalidToken
                );

        String passwordHash = passwordEncoder.encode(
                request.newPassword()
        );

        boolean tokenMarkedUsed = tokenRepository.markUsed(
                resetToken.id()
        );

        if (!tokenMarkedUsed) {
            throw invalidToken();
        }

        boolean passwordUpdated =
                userRepository.updatePasswordHashById(
                        user.id(),
                        passwordHash
                );

        if (!passwordUpdated) {
            throw new IllegalStateException(
                    "Could not update user password"
            );
        }

        invalidateSessions(user.email());

        log.info(
                "Password reset completed userId={}",
                user.id()
        );
    }

    private void invalidateSessions(String email) {
        Map<String, ? extends Session> sessions =
                sessionRepository.findByPrincipalName(email);

        sessions.keySet().forEach(
                sessionRepository::deleteById
        );
    }

    private static String generateToken() {
        byte[] tokenBytes = new byte[32];

        SECURE_RANDOM.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

    private static String hashToken(String token) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is not available",
                    exception
            );
        }
    }

    private static InvalidPasswordResetTokenException
    invalidToken() {
        return new InvalidPasswordResetTokenException(
                "Password reset token is invalid or expired"
        );
    }
}
