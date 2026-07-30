package com.example.imagehostingservice.auth.service;

import com.example.imagehostingservice.auth.dto.LoginRequest;
import com.example.imagehostingservice.auth.dto.RegisterRequest;
import com.example.imagehostingservice.auth.dto.AuthenticatedUserResponse;
import com.example.imagehostingservice.exception.EmailAlreadyExistsException;
import com.example.imagehostingservice.exception.InvalidCredentialsException;
import com.example.imagehostingservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.example.imagehostingservice.user.model.User;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthenticatedUserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already in use");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User createdUser;

        try {
            createdUser = userRepository.save(
                    request.name().trim(),
                    email,
                    passwordHash
            );
        } catch (DuplicateKeyException exception) {
            throw new EmailAlreadyExistsException(
                    "Email is already in use"
            );
        }
        log.info(
                "User registered userId={}",
                createdUser.id()
        );
        authenticate(email, request.password());

        return new AuthenticatedUserResponse(
                createdUser.publicId(),
                createdUser.name(),
                createdUser.email(),
                createdUser.createdAt()
        );
    }

    public AuthenticatedUserResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        authenticate(email, request.password());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password")
                );
        log.info(
                "User authenticated userId={}",
                user.id()
        );


        return new AuthenticatedUserResponse(
                user.publicId(),
                user.name(),
                user.email(),
                user.createdAt()
        );
    }
    public AuthenticatedUserResponse getCurrentUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        return new AuthenticatedUserResponse(
                user.publicId(),
                user.name(),
                user.email(),
                user.createdAt()
        );
    }
    private void authenticate(String email, String password) {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            email,
                            password
                    )
            );
        } catch (AuthenticationException exception) {
            log.warn("Authentication failed");

            throw new InvalidCredentialsException(
                    "Invalid email or password"
            );
        }

        SecurityContext securityContext =
                SecurityContextHolder.createEmptyContext();

        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

}
