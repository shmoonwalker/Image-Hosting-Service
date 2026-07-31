package com.example.imagehostingservice.sharing.dto;

import com.example.imagehostingservice.sharing.model.ShareExpiration;
import jakarta.validation.constraints.NotNull;

public record CreateShareLinkRequest(
        @NotNull
        ShareExpiration expiration
) {
}
