package com.borae.identity;

import jakarta.validation.constraints.NotBlank;

public record AcceptRestaurantInvitationRequest(@NotBlank String token) {
}
