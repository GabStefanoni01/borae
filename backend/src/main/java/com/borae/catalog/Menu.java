package com.borae.catalog;

import com.borae.identity.Restaurant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "restaurant_menu")
public class Menu {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MenuStatus status;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected Menu() {
    }

    public Menu(Restaurant restaurant, String name, String description, int displayOrder) {
        this.id = UUID.randomUUID();
        this.restaurant = restaurant;
        this.name = name;
        this.description = description;
        this.status = MenuStatus.DRAFT;
        this.displayOrder = displayOrder;
    }

    public UUID getId() { return id; }
    public Restaurant getRestaurant() { return restaurant; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public MenuStatus getStatus() { return status; }
    public int getDisplayOrder() { return displayOrder; }

    public void update(String name, String description, int displayOrder) {
        ensureNotArchived();
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public void publish() {
        ensureNotArchived();
        this.status = MenuStatus.PUBLISHED;
    }

    public void unpublish() {
        ensureNotArchived();
        this.status = MenuStatus.DRAFT;
    }

    public void archive() {
        this.status = MenuStatus.ARCHIVED;
    }

    private void ensureNotArchived() {
        if (status == MenuStatus.ARCHIVED) {
            throw new IllegalStateException("Cardápio arquivado não pode ser alterado.");
        }
    }
}
