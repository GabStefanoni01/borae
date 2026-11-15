package com.borae.catalog;

import java.util.List;
import java.util.UUID;

public record MenuProductResponse(
        UUID id,
        UUID menuId,
        UUID categoryId,
        String name,
        String description,
        long priceInCents,
        ProductLifecycleStatus lifecycleStatus,
        ProductAvailability availability,
        int displayOrder,
        List<String> ingredients,
        List<AddonResponse> addons
) {
    public record AddonResponse(String name, long priceDeltaInCents, boolean active) {}

    public static MenuProductResponse from(MenuProduct product) {
        return new MenuProductResponse(
                product.getId(),
                product.getCategory().getMenu().getId(),
                product.getCategory().getId(),
                product.getName(),
                product.getDescription(),
                product.getPriceInCents(),
                product.getLifecycleStatus(),
                product.getAvailability(),
                product.getDisplayOrder(),
                product.getIngredients(),
                product.getAddons().stream()
                        .map(addon -> new AddonResponse(addon.getName(), addon.getPriceDeltaInCents(), addon.isActive()))
                        .toList()
        );
    }
}
