package com.borae.identity;

import java.time.Instant;
import java.util.UUID;

public record RestaurantInvitationResponse(
        UUID invitationId,
        String email,
        RestaurantRole role,
        Instant expiresAt,
        String token
) {
}
