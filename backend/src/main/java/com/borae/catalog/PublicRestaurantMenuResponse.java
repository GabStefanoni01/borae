package com.borae.catalog;

import com.borae.identity.Restaurant;
import java.util.List;
import java.util.UUID;

public record PublicRestaurantMenuResponse(
        UUID id,
        String name,
        String slug,
        String description,
        String phone,
        String whatsapp,
        String instagram,
        String website,
        String category,
        String address,
        String logoUrl,
        List<PublicMenu> menus
) {
    public record PublicMenu(UUID id, String name, String description, int displayOrder,
                             List<PublicCategory> categories) {}

    public record PublicCategory(UUID id, String name, String description, int displayOrder,
                                 List<PublicProduct> products) {}

    public record PublicProduct(UUID id, String name, String description, long priceInCents,
                                ProductAvailability availability, int displayOrder,
                                List<String> ingredients, List<PublicAddon> addons, String imageUrl, Public3DModel model3d) {}

    public record Public3DModel(String modelUrl, String contentType,
                                int widthMm, int heightMm, int depthMm) {}

    public record PublicAddon(String name, long priceDeltaInCents) {}

    public static PublicRestaurantMenuResponse from(Restaurant restaurant, List<PublicMenu> menus) {
        return new PublicRestaurantMenuResponse(
                restaurant.getId(), restaurant.getName(), restaurant.getSlug(), restaurant.getDescription(),
                restaurant.getPhone(), restaurant.getWhatsapp(), restaurant.getInstagram(),
                restaurant.getWebsite(), restaurant.getCategory(), restaurant.getAddress(), null, List.copyOf(menus)
        );
    }
}
