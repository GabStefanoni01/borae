package com.borae.identity;

import java.util.UUID;

public record RestaurantProfileResponse(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active
) {
    public static RestaurantProfileResponse from(Restaurant restaurant) {
        return new RestaurantProfileResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getSlug(),
                restaurant.getDescription(),
                restaurant.isActive()
        );
    }
}
