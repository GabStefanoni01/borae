package com.borae.catalog;

import com.borae.identity.UserAccount;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_3d_generation_job")
public class Product3DGenerationJob {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private MenuProduct product;

    @Column(name = "source_media_id", nullable = false)
    private UUID sourceMediaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private UserAccount requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Product3DGenerationJobStatus status;

    @Column(length = 40)
    private String engine;

    @Column(name = "error_code", length = 60)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Product3DGenerationJob() {}

    public Product3DGenerationJob(MenuProduct product, UUID sourceMediaId, UserAccount requestedBy) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.sourceMediaId = sourceMediaId;
        this.requestedBy = requestedBy;
        this.status = Product3DGenerationJobStatus.QUEUED;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public MenuProduct getProduct() { return product; }
    public UUID getSourceMediaId() { return sourceMediaId; }
    public UserAccount getRequestedBy() { return requestedBy; }
    public Product3DGenerationJobStatus getStatus() { return status; }
    public String getEngine() { return engine; }
    public String getErrorCode() { return errorCode; }
    public String getErrorMessage() { return errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void start(String engine) {
        if (status != Product3DGenerationJobStatus.QUEUED) {
            throw new IllegalStateException("Only queued generation jobs can start.");
        }
        this.status = Product3DGenerationJobStatus.PROCESSING;
        this.engine = engine;
        this.startedAt = Instant.now();
        this.updatedAt = this.startedAt;
    }

    public void complete() {
        if (status != Product3DGenerationJobStatus.PROCESSING) {
            throw new IllegalStateException("Only processing generation jobs can complete.");
        }
        this.status = Product3DGenerationJobStatus.COMPLETED;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
        this.errorCode = null;
        this.errorMessage = null;
    }

    public void fail(String code, String message) {
        if (status != Product3DGenerationJobStatus.PROCESSING
                && status != Product3DGenerationJobStatus.QUEUED) {
            throw new IllegalStateException("Only active generation jobs can fail.");
        }
        this.status = Product3DGenerationJobStatus.FAILED;
        this.errorCode = code;
        this.errorMessage = message;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }

    public void cancel() {
        if (status != Product3DGenerationJobStatus.QUEUED) {
            throw new IllegalStateException("Only queued generation jobs can be cancelled.");
        }
        this.status = Product3DGenerationJobStatus.CANCELLED;
        this.completedAt = Instant.now();
        this.updatedAt = this.completedAt;
    }
}
