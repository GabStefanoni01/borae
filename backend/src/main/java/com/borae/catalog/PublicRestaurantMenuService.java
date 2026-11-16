package com.borae.catalog;

import com.borae.identity.Restaurant;
import com.borae.identity.RestaurantPublicationStatus;
import com.borae.identity.RestaurantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class PublicRestaurantMenuService {

    private final RestaurantRepository restaurantRepository;
    private final MenuRepository menuRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuProductRepository productRepository;

    public PublicRestaurantMenuService(RestaurantRepository restaurantRepository,
                                       MenuRepository menuRepository,
                                       MenuCategoryRepository categoryRepository,
                                       MenuProductRepository productRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public PublicRestaurantMenuResponse getBySlug(String slug) {
        Restaurant restaurant = restaurantRepository
                .findBySlugAndActiveTrueAndPublicationStatus(slug, RestaurantPublicationStatus.PUBLISHED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Restaurante não encontrado ou não publicado."));

        List<PublicRestaurantMenuResponse.PublicMenu> menus = menuRepository
                .findAllByRestaurant_IdAndStatusOrderByDisplayOrderAscNameAsc(
                        restaurant.getId(), MenuStatus.PUBLISHED)
                .stream()
                .map(menu -> {
                    List<PublicRestaurantMenuResponse.PublicCategory> categories = categoryRepository
                            .findAllByMenu_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(menu.getId())
                            .stream()
                            .map(category -> {
                                List<PublicRestaurantMenuResponse.PublicProduct> products = productRepository
                                        .findAllByCategory_IdAndLifecycleStatusOrderByDisplayOrderAscNameAsc(
                                                category.getId(), ProductLifecycleStatus.ACTIVE)
                                        .stream()
                                        .map(product -> new PublicRestaurantMenuResponse.PublicProduct(
                                                product.getId(),
                                                product.getName(),
                                                product.getDescription(),
                                                product.getPriceInCents(),
                                                product.getAvailability(),
                                                product.getDisplayOrder(),
                                                product.getIngredients(),
                                                product.getAddons().stream()
                                                        .filter(MenuProductAddon::isActive)
                                                        .map(addon -> new PublicRestaurantMenuResponse.PublicAddon(
                                                                addon.getName(), addon.getPriceDeltaInCents()))
                                                        .toList(),
                                                product.getImage() == null ? null : product.getImage().getPublicUrl()
                                        ))
                                        .toList();
                                return new PublicRestaurantMenuResponse.PublicCategory(
                                        category.getId(), category.getName(), category.getDescription(),
                                        category.getDisplayOrder(), products);
                            })
                            .toList();
                    return new PublicRestaurantMenuResponse.PublicMenu(
                            menu.getId(), menu.getName(), menu.getDescription(), menu.getDisplayOrder(), categories);
                })
                .toList();

        return PublicRestaurantMenuResponse.from(restaurant, menus);
    }
}
