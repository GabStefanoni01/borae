package com.borae.catalog;

import java.util.UUID;

public record MenuCategoryResponse(
        UUID id,
        UUID menuId,
        String name,
        String description,
        boolean active,
        int displayOrder
) {
    public static MenuCategoryResponse from(MenuCategory category) {
        return new MenuCategoryResponse(category.getId(), category.getMenu().getId(),
                category.getName(), category.getDescription(), category.isActive(),
                category.getDisplayOrder());
    }
}
