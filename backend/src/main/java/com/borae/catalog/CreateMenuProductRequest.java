package com.borae.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateMenuProductRequest(
        @NotNull UUID categoryId,
        @NotBlank @Size(max = 160) String name,
        @Size(max = 1000) String description,
        @NotNull @PositiveOrZero Long priceInCents,
        @NotNull @PositiveOrZero Integer displayOrder,
        @NotNull @Size(max = 40) List<@NotBlank @Size(max = 120) String> ingredients,
        @NotNull @Size(max = 30) List<@NotNull @Valid ProductAddonRequest> addons
) {
}
