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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

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
    void registerShouldCreateUserAndReturnResponse() {
        RegisterRequest request = new RegisterRequest(
                "Shadi",
                " SHADI@EXAMPLE.COM ",
                "password123"
        );

        User savedUser = new User(
                1L,
                "Shadi",
                "shadi@example.com",
                "bcrypt-hash",
                LocalDateTime.of(2026, 7, 17, 12, 0)
        );

        when(userRepository.existsByEmail("shadi@example.com"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123"))
                .thenReturn("bcrypt-hash");
        when(userRepository.save(
                "Shadi",
                "shadi@example.com",
                "bcrypt-hash"
        )).thenReturn(savedUser);

        AuthenticatedUserResponse response = authService.register(request);

        assertEquals(1L, response.id());
        assertEquals("Shadi", response.name());
        assertEquals("shadi@example.com", response.email());

        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(
                "Shadi",
                "shadi@example.com",
                "bcrypt-hash"
        );
    }

    @Test
    void registerShouldThrowExceptionWhenEmailAlreadyExists() {
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
    }

    @Test
    void loginShouldAuthenticateUserAndStoreSecurityContext() {
        LoginRequest request = new LoginRequest(
                " SHADI@EXAMPLE.COM ",
                "password123"
        );

        Authentication authenticatedUser = org.mockito.Mockito.mock(
                Authentication.class
        );

        User user = new User(
                1L,
                "Shadi",
                "shadi@example.com",
                "bcrypt-hash",
                LocalDateTime.of(2026, 7, 17, 12, 0)
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenReturn(authenticatedUser);
        when(userRepository.findByEmail("shadi@example.com"))
                .thenReturn(Optional.of(user));

        AuthenticatedUserResponse response = authService.login(request);

        assertEquals(1L, response.id());
        assertEquals("Shadi", response.name());
        assertEquals("shadi@example.com", response.email());

        assertSame(
                authenticatedUser,
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
    void loginShouldThrowExceptionWhenCredentialsAreInvalid() {
        LoginRequest request = new LoginRequest(
                "shadi@example.com",
                "wrong-password"
        );

        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        assertThrows(
                InvalidCredentialsException.class,
                () -> authService.login(request)
        );

        verify(userRepository, never()).findByEmail(any());
    }
}