package com.borae.catalog;

import java.time.Instant;
import java.util.UUID;

public record Product3DGenerationJobResponse(
        UUID id,
        UUID productId,
        Product3DGenerationJobStatus status,
        String engine,
        String errorCode,
        String errorMessage,
        Instant createdAt,
        Instant startedAt,
        Instant completedAt,
        Instant updatedAt
) {
    public static Product3DGenerationJobResponse from(Product3DGenerationJob job) {
        return new Product3DGenerationJobResponse(
                job.getId(),
                job.getProduct().getId(),
                job.getStatus(),
                job.getEngine(),
                job.getErrorCode(),
                job.getErrorMessage(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                job.getUpdatedAt()
        );
    }
}
