package com.borae.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    protected Restaurant() {
    }

    public Restaurant(String name, String slug, String description) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.active = true;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public boolean isActive() { return active; }
}
