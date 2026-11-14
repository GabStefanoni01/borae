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

    @Column(name = "object_key", nullable = false, unique = true, length = 500)
    private String objectKey;

    @Column(name = "public_url", length = 1000)
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

    public RestaurantMedia(Restaurant restaurant, RestaurantMediaType mediaType, String objectKey,
                           String publicUrl, String contentType, long fileSize, int width, int height) {
        this.id = UUID.randomUUID();
        this.restaurant = restaurant;
        this.mediaType = mediaType;
        this.objectKey = objectKey;
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
    public String getObjectKey() { return objectKey; }
    public String getPublicUrl() { return publicUrl; }
    public String getContentType() { return contentType; }
    public long getFileSize() { return fileSize; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public Instant getCreatedAt() { return createdAt; }
}
