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

import java.util.UUID;

@Entity
@Table(name = "restaurant_membership")
public class RestaurantMembership {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RestaurantRole role;

    @Column(nullable = false)
    private boolean active;

    protected RestaurantMembership() {
    }

    public UUID getId() { return id; }
    public UserAccount getUser() { return user; }
    public Restaurant getRestaurant() { return restaurant; }
    public RestaurantRole getRole() { return role; }
    public boolean isActive() { return active; }
}
