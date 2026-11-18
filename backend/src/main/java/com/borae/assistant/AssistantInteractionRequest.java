package com.borae.assistant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AssistantInteractionRequest(
        @NotBlank @Size(max = 500) String message,
        UUID menuId,
        @Valid Filters filters
) {
    public record Filters(
            @PositiveOrZero @Max(10_000_000) Long maxPriceInCents,
            @Size(max = 20) List<UUID> categoryIds,
            @Size(max = 20) List<@NotBlank @Size(max = 60) String> excludedIngredients
    ) {}
}
