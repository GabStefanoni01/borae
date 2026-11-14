package com.borae.identity;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateRestaurantProfileRequest(
        @Size(min = 1, max = 180) String name,
        @Size(max = 1000) String description,
        @Size(min = 1, max = 120)
        @Pattern(regexp = "^[a-zA-Z0-9À-ÿ-]+$", message = "Use apenas letras, números e hífens no endereço público.")
        String slug,
        @Size(max = 30) String phone,
        @Size(max = 30) String whatsapp,
        @Size(max = 120) String instagram,
        @Size(max = 255) String website,
        @Size(max = 500) String address,
        @Digits(integer = 3, fraction = 7)
        @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal latitude,
        @Digits(integer = 3, fraction = 7)
        @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal longitude,
        @Size(max = 80) String category
) {
}
