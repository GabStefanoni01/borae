package com.borae.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restaurant_media")
public class RestaurantMedia {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 16)
    private RestaurantMediaType mediaType;

    @Column(name = "provider_file_id", nullable = false, unique = true, length = 200)
    private String providerFileId;

    @Column(name = "public_url", nullable = false, length = 1000)
    private String publicUrl;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private int width;

    @Column(nullable = false)
    private int height;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected RestaurantMedia() {}

    public RestaurantMedia(Restaurant restaurant, RestaurantMediaType mediaType, String providerFileId,
                           String publicUrl, String contentType, long fileSize, int width, int height) {
        this.id = UUID.randomUUID();
        this.restaurant = restaurant;
        this.mediaType = mediaType;
        this.providerFileId = providerFileId;
        this.publicUrl = publicUrl;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.width = width;
        this.height = height;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public Restaurant getRestaurant() { return restaurant; }
    public RestaurantMediaType getMediaType() { return mediaType; }
    public String getProviderFileId() { return providerFileId; }
    public String getPublicUrl() { return publicUrl; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Instant getCreatedAt() { return createdAt; }
}
