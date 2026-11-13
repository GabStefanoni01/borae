package com.borae.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserSummary user
) {
    public record UserSummary(
            UUID id,
            String fullName,
            String email
    ) {
    }
}
