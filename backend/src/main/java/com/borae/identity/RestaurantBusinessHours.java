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
import jakarta.persistence.UniqueConstraint;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(
        name = "restaurant_business_hours",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_restaurant_business_hours_position",
                columnNames = {"restaurant_id", "day_of_week", "position"}
        )
)
public class RestaurantBusinessHours {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 16)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private int position;

    @Column(name = "opening_time", nullable = false)
    private LocalTime openingTime;

    @Column(name = "closing_time", nullable = false)
    private LocalTime closingTime;

    protected RestaurantBusinessHours() {
    }

    public RestaurantBusinessHours(
            Restaurant restaurant,
            DayOfWeek dayOfWeek,
            int position,
            LocalTime openingTime,
            LocalTime closingTime
    ) {
        this.id = UUID.randomUUID();
        this.restaurant = restaurant;
        this.dayOfWeek = dayOfWeek;
        this.position = position;
        this.openingTime = openingTime;
        this.closingTime = closingTime;
    }

    public UUID getId() { return id; }
    public Restaurant getRestaurant() { return restaurant; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public int getPosition() { return position; }
    public LocalTime getOpeningTime() { return openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
}
