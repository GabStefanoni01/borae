package com.borae.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductAddonRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull @PositiveOrZero Long priceDeltaInCents,
        @NotNull Boolean active
) {
}
