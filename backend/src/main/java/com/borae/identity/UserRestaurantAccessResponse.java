package com.borae.identity;

import java.util.UUID;

public record UserRestaurantAccessResponse(
        UUID membershipId,
        UUID restaurantId,
        String restaurantName,
        String restaurantSlug,
        RestaurantRole role
) {
    public static UserRestaurantAccessResponse from(RestaurantMembership membership) {
        Restaurant restaurant = membership.getRestaurant();
        return new UserRestaurantAccessResponse(
                membership.getId(),
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getSlug(),
                membership.getRole()
        );
    }
}
