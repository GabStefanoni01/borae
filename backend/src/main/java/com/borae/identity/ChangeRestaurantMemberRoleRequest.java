package com.borae.identity;

import jakarta.validation.constraints.NotNull;

public record ChangeRestaurantMemberRoleRequest(@NotNull RestaurantRole role) {
}
