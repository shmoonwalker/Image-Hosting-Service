package com.example.imagehostingservice.auth.service;

import com.example.imagehostingservice.auth.dto.AuthenticatedUserResponse;
import com.example.imagehostingservice.auth.dto.LoginRequest;
import com.example.imagehostingservice.auth.dto.RegisterRequest;
import com.example.imagehostingservice.exception.EmailAlreadyExistsException;
import com.example.imagehostingservice.exception.InvalidCredentialsException;
import com.example.imagehostingservice.user.model.User;
import com.example.imagehostingservice.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void registerShouldCreateAuthenticateAndReturnUser() {
        RegisterRequest request = new RegisterRequest(
                " Shadi ",
                " SHADI@EXAMPLE.COM ",
                "password123"
        );

        UUID publicId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.of(
                2026,
                7,
                17,
                12,
                0,
                0,
                0,
                ZoneOffset.UTC
        );

        User savedUser = new User(
                1L,
                publicId,
                "Shadi",
                "shadi@example.com",
                "bcrypt-hash",
                createdAt
        );

        Authentication authentication =
                org.mockito.Mockito.mock(Authentication.class);

        when(userRepository.existsByEmail("shadi@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("bcrypt-hash");

        when(userRepository.save(
                "Shadi",
                "shadi@example.com",
                "bcrypt-hash"
        )).thenReturn(savedUser);

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        AuthenticatedUserResponse response = authService.register(request);

        assertEquals(publicId, response.id());
        assertEquals("Shadi", response.name());
        assertEquals("shadi@example.com", response.email());
        assertEquals(createdAt, response.createdAt());

        assertSame(
                authentication,
                SecurityContextHolder.getContext().getAuthentication()
        );

        verify(passwordEncoder).encode("password123");

        verify(userRepository).save(
                "Shadi",
                "shadi@example.com",
                "bcrypt-hash"
        );

        ArgumentCaptor<Authentication> authenticationCaptor =
                ArgumentCaptor.forClass(Authentication.class);

        verify(authenticationManager).authenticate(
                authenticationCaptor.capture()
        );

        assertEquals(
                "shadi@example.com",
                authenticationCaptor.getValue().getName()
        );

        assertEquals(
                "password123",
                authenticationCaptor.getValue().getCredentials()
        );
    }

    @Test
    void registerShouldRejectExistingEmail() {
        RegisterRequest request = new RegisterRequest(
                "Shadi",
                "shadi@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("shadi@example.com"))
                .thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any(), any(), any());
        verify(authenticationManager, never())
                .authenticate(any(Authentication.class));
    }

    @Test
    void registerShouldHandleDuplicateEmailRaceCondition() {
        RegisterRequest request = new RegisterRequest(
                "Shadi",
                "shadi@example.com",
                "password123"
        );

        when(userRepository.existsByEmail("shadi@example.com"))
                .thenReturn(false);

        when(passwordEncoder.encode("password123"))
                .thenReturn("bcrypt-hash");

        when(userRepository.save(
                "Shadi",
                "shadi@example.com",
                "bcrypt-hash"
        )).thenThrow(new DuplicateKeyException("Duplicate email"));

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.register(request)
        );

        verify(authenticationManager, never())
                .authenticate(any(Authentication.class));
    }

    @Test
    void loginShouldAuthenticateAndReturnUser() {
        LoginRequest request = new LoginRequest(
                " SHADI@EXAMPLE.COM ",
                "password123"
        );

        Authentication authentication =
                org.mockito.Mockito.mock(Authentication.class);

        UUID publicId = UUID.randomUUID();
        OffsetDateTime createdAt = OffsetDateTime.of(
                2026,
                7,
                17,
                12,
                0,
                0,
                0,
                ZoneOffset.UTC
        );

        User user = new User(
                1L,
                publicId,
                "Shadi",
                "shadi@example.com",
                "bcrypt-hash",
                createdAt
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authentication);

        when(userRepository.findByEmail("shadi@example.com"))
                .thenReturn(Optional.of(user));

        AuthenticatedUserResponse response = authService.login(request);

        assertEquals(publicId, response.id());
        assertEquals("Shadi", response.name());
        assertEquals("shadi@example.com", response.email());
        assertEquals(createdAt, response.createdAt());

        assertSame(
                authentication,
                SecurityContextHolder.getContext().getAuthentication()
        );

        ArgumentCaptor<Authentication> authenticationCaptor =
                ArgumentCaptor.forClass(Authentication.class);

        verify(authenticationManager).authenticate(
                authenticationCaptor.capture()
        );

        assertEquals(
                "shadi@example.com",
                authenticationCaptor.getValue().getName()
        );

        assertEquals(
                "password123",
                authenticationCaptor.getValue().getCredentials()
        );
    }

    @Test
    void loginShouldRejectInvalidCredentials() {
        LoginRequest request = new LoginRequest(
                "shadi@example.com",
                "wrong-password"
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(
                        new BadCredentialsException("Invalid credentials")
                );

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(userRepository, never()).findByEmail(any());
    }
}