package com.borae.identity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRestaurantRequest(
        @NotBlank @Size(max = 180) String name,
        @Size(max = 1000) String description
) {
}
