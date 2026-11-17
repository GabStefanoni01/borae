package com.borae.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "menu_product_3d_model")
public class Product3DModel {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private MenuProduct product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Product3DModelStatus status;

    @Column(name = "provider_file_id", nullable = false, unique = true, length = 200)
    private String providerFileId;

    @Column(name = "public_url", nullable = false, length = 1000)
    private String publicUrl;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "width_mm", nullable = false)
    private int widthMm;

    @Column(name = "height_mm", nullable = false)
    private int heightMm;

    @Column(name = "depth_mm", nullable = false)
    private int depthMm;

    @Column(name = "created_by_user_id", nullable = false)
    private UUID createdByUserId;

    @Column(name = "updated_by_user_id", nullable = false)
    private UUID updatedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected Product3DModel() {}

    public Product3DModel(MenuProduct product, String providerFileId, String publicUrl,
                          String contentType, long fileSize, int widthMm, int heightMm,
                          int depthMm, UUID actorId) {
        this.id = UUID.randomUUID();
        this.product = product;
        this.providerFileId = providerFileId;
        this.publicUrl = publicUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.depthMm = depthMm;
        this.status = Product3DModelStatus.READY;
        this.createdByUserId = actorId;
        this.updatedByUserId = actorId;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() { return id; }
    public MenuProduct getProduct() { return product; }
    public Product3DModelStatus getStatus() { return status; }
    public String getProviderFileId() { return providerFileId; }
    public String getPublicUrl() { return publicUrl; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public int getWidthMm() { return widthMm; }
    public int getHeightMm() { return heightMm; }
    public int getDepthMm() { return depthMm; }
    public UUID getCreatedByUserId() { return createdByUserId; }
    public UUID getUpdatedByUserId() { return updatedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public String replace(String providerFileId, String publicUrl, String contentType, long fileSize,
                           int widthMm, int heightMm, int depthMm, UUID actorId) {
        String previousFileId = this.providerFileId;
        this.providerFileId = providerFileId;
        this.publicUrl = publicUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.widthMm = widthMm;
        this.heightMm = heightMm;
        this.depthMm = depthMm;
        this.status = Product3DModelStatus.READY;
        this.updatedByUserId = actorId;
        this.updatedAt = Instant.now();
        return previousFileId;
    }

    public void publish(UUID actorId) {
        this.status = Product3DModelStatus.PUBLISHED;
        this.updatedByUserId = actorId;
        this.updatedAt = Instant.now();
    }

    public void unpublish(UUID actorId) {
        this.status = Product3DModelStatus.READY;
        this.updatedByUserId = actorId;
        this.updatedAt = Instant.now();
    }
}
