package com.borae.identity;

import java.math.BigDecimal;
import java.util.UUID;

public record RestaurantProfileResponse(
        UUID id,
        String name,
        String slug,
        String description,
        boolean active,
        String phone,
        String whatsapp,
        String instagram,
        String website,
        String address,
        BigDecimal latitude,
        BigDecimal longitude,
        String category,
        RestaurantPublicationStatus publicationStatus
) {
    public static RestaurantProfileResponse from(Restaurant restaurant) {
        return new RestaurantProfileResponse(
                restaurant.getId(),
                restaurant.getName(),
                restaurant.getSlug(),
                restaurant.getDescription(),
                restaurant.isActive(),
                restaurant.getPhone(),
                restaurant.getWhatsapp(),
                restaurant.getInstagram(),
                restaurant.getWebsite(),
                restaurant.getAddress(),
                restaurant.getLatitude(),
                restaurant.getLongitude(),
                restaurant.getCategory(),
                restaurant.getPublicationStatus()
        );
    }
}
