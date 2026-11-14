package com.borae.identity;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantProfileRequest(
        @Size(min = 1, max = 180) String name,
        @Size(max = 1000) String description,
        @Size(min = 1, max = 120)
        @Pattern(regexp = "^[a-zA-Z0-9À-ÿ-]+$", message = "Use apenas letras, números e hífens no endereço público.")
        String slug
) {
}
