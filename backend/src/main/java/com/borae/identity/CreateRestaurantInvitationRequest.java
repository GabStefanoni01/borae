package com.borae.identity;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRestaurantInvitationRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull RestaurantRole role
) {
}
