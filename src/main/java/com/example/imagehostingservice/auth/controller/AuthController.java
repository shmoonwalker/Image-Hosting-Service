package com.example.imagehostingservice.auth.controller;

import com.example.imagehostingservice.auth.dto.LoginRequest;
import com.example.imagehostingservice.auth.dto.RegisterRequest;
import com.example.imagehostingservice.auth.dto.AuthenticatedUserResponse;
import com.example.imagehostingservice.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;

@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@RestController
public class AuthController {
    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/register")
    public ResponseEntity<AuthenticatedUserResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthenticatedUserResponse response = authService.register(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticatedUserResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    )
        {

        AuthenticatedUserResponse response = authService.login(request);
            securityContextRepository.saveContext(
                    SecurityContextHolder.getContext(),
                    httpRequest,
                    httpResponse
            );
            return ResponseEntity.ok(response);
        }

    @GetMapping("/me")
    public ResponseEntity<AuthenticatedUserResponse> getCurrentUser(
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                authService.getCurrentUser(authentication.getName())
        );
    }

    @GetMapping("/csrf")
    public CsrfToken csrfToken(CsrfToken csrfToken) {
        return csrfToken;
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        SecurityContextLogoutHandler securityContextLogoutHandler =
                new SecurityContextLogoutHandler();

        securityContextLogoutHandler.setSecurityContextRepository(
                securityContextRepository
        );

        securityContextLogoutHandler.logout(
                request,
                response,
                authentication
        );

        new CookieClearingLogoutHandler("SESSION").logout(
                request,
                response,
                authentication
        );

        return ResponseEntity.noContent().build();
    }

}
