package com.borae.assistant;

import com.borae.catalog.ProductAvailability;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

public record AssistantInteractionResponse(
        String answer,
        List<Recommendation> recommendations,
        Quota quota,
        boolean fallback,
        String fallbackCode
) {
    public record Recommendation(
            UUID productId,
            String name,
            long priceInCents,
            ProductAvailability availability,
            String imageUrl,
            List<String> ingredients,
            String reason
    ) {}

    public record Quota(int remainingConsumer, int remainingRestaurant, ZonedDateTime resetsAt) {}
}
