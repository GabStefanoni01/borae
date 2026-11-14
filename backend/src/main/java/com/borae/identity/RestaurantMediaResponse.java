package com.borae.identity;

import java.time.Instant;
import java.util.UUID;

public record RestaurantMediaResponse(
        UUID id,
        RestaurantMediaType type,
        String url,
        String contentType,
        long sizeBytes,
        int width,
        int height,
        Instant createdAt
) {
    public static RestaurantMediaResponse from(RestaurantMedia media) {
        return new RestaurantMediaResponse(media.getId(), media.getMediaType(), media.getPublicUrl(),
                media.getContentType(), media.getFileSize(), media.getWidth(), media.getHeight(), media.getCreatedAt());
    }
}
