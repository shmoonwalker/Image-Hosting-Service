package com.example.imagehostingservice.auth.controller;

import com.example.imagehostingservice.auth.dto.LoginRequest;
import com.example.imagehostingservice.auth.dto.RegisterRequest;
import com.example.imagehostingservice.auth.dto.AuthenticatedUserResponse;
import com.example.imagehostingservice.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
@RestController
public class AuthController {
    private final AuthService authService;

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
            @Valid @RequestBody LoginRequest request
    )
        {
        AuthenticatedUserResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        }

}
