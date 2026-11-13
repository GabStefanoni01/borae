package com.borae.identity;

import java.util.UUID;

public record RestaurantMemberResponse(
        UUID membershipId,
        UUID restaurantId,
        UUID userId,
        String fullName,
        String email,
        RestaurantRole role
) {
    public static RestaurantMemberResponse from(RestaurantMembership membership) {
        UserAccount user = membership.getUser();
        return new RestaurantMemberResponse(
                membership.getId(),
                membership.getRestaurant().getId(),
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                membership.getRole()
        );
    }
}
