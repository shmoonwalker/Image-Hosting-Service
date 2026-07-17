package com.example.imagehostingservice.auth.service;

import com.example.imagehostingservice.auth.dto.LoginRequest;
import com.example.imagehostingservice.auth.dto.RegisterRequest;
import com.example.imagehostingservice.auth.dto.AuthenticatedUserResponse;
import com.example.imagehostingservice.exception.EmailAlreadyExistsException;
import com.example.imagehostingservice.exception.InvalidCredentialsException;
import com.example.imagehostingservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import com.example.imagehostingservice.user.model.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    public AuthenticatedUserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException("Email is already in use");
        }

        String passwordHash = passwordEncoder.encode(request.password());

        User createdUser = userRepository.save(
                request.name().trim(),
                email,
                passwordHash
        );

        return new AuthenticatedUserResponse(
                createdUser.id(),
                createdUser.name(),
                createdUser.email(),
                createdUser.createdAt()
        );
    }
    public AuthenticatedUserResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password")
                );

        if (!passwordEncoder.matches(request.password(), user.passwordHash())) {
            throw new InvalidCredentialsException("Invalid email or password");
        }

        return new AuthenticatedUserResponse(
                user.id(),
                user.name(),
                user.email(),
                user.createdAt()
        );
    }
}
