package com.borae.catalog;

import java.time.Instant;
import java.util.UUID;

public record Product3DModelResponse(
        UUID id,
        UUID productId,
        Product3DModelStatus status,
        String modelUrl,
        String contentType,
        long fileSize,
        int widthMm,
        int heightMm,
        int depthMm,
        Instant createdAt,
        Instant updatedAt
) {
    public static Product3DModelResponse from(Product3DModel model) {
        return new Product3DModelResponse(model.getId(), model.getProduct().getId(), model.getStatus(),
                model.getPublicUrl(), model.getContentType(), model.getFileSize(),
                model.getWidthMm(), model.getHeightMm(), model.getDepthMm(),
                model.getCreatedAt(), model.getUpdatedAt());
    }
}
