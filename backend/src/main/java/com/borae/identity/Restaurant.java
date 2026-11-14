package com.borae.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "restaurant")
public class Restaurant {

    @Id
    private UUID id;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private boolean active;

    @Column(length = 30)
    private String phone;

    @Column(length = 30)
    private String whatsapp;

    @Column(length = 120)
    private String instagram;

    @Column(length = 255)
    private String website;

    @Column(length = 500)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 80)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "publication_status", nullable = false, length = 20)
    private RestaurantPublicationStatus publicationStatus;

    protected Restaurant() {
    }

    public Restaurant(String name, String slug, String description) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.active = true;
        this.publicationStatus = RestaurantPublicationStatus.DRAFT;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
    public String getPhone() { return phone; }
    public String getWhatsapp() { return whatsapp; }
    public String getInstagram() { return instagram; }
    public String getWebsite() { return website; }
    public String getAddress() { return address; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public String getCategory() { return category; }
    public RestaurantPublicationStatus getPublicationStatus() { return publicationStatus; }

    public void updateProfile(
            String name,
            String slug,
            String description,
            String phone,
            String whatsapp,
            String instagram,
            String website,
            String address,
            BigDecimal latitude,
            BigDecimal longitude,
            String category
    ) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.phone = phone;
        this.whatsapp = whatsapp;
        this.instagram = instagram;
        this.website = website;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.category = category;
    }
}
