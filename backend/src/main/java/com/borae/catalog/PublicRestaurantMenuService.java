package com.borae.catalog;

import com.borae.identity.Restaurant;
import com.borae.identity.RestaurantPublicationStatus;
import com.borae.identity.RestaurantRepository;
import com.borae.identity.RestaurantMediaRepository;
import com.borae.identity.RestaurantMediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PublicRestaurantMenuService {

    private final RestaurantRepository restaurantRepository;
    private final MenuRepository menuRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuProductRepository productRepository;
    private final Product3DModelRepository modelRepository;
    private final RestaurantMediaRepository mediaRepository;

    public PublicRestaurantMenuService(RestaurantRepository restaurantRepository,
                                       MenuRepository menuRepository,
                                       MenuCategoryRepository categoryRepository,
                                       MenuProductRepository productRepository,
                                       Product3DModelRepository modelRepository,
                                       RestaurantMediaRepository mediaRepository) {
        this.restaurantRepository = restaurantRepository;
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.modelRepository = modelRepository;
        this.mediaRepository = mediaRepository;
    }

    @Transactional(readOnly = true)
    public PublicRestaurantMenuResponse getBySlug(String slug) {
        String normalizedSlug = slug == null ? "" : slug.trim().toLowerCase(Locale.ROOT);
        Restaurant restaurant = restaurantRepository
                .findBySlugAndActiveTrueAndPublicationStatus(normalizedSlug, RestaurantPublicationStatus.PUBLISHED)
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
                                List<MenuProduct> products = productRepository
                                        .findAllByCategory_IdAndLifecycleStatusOrderByDisplayOrderAscNameAsc(
                                                category.getId(), ProductLifecycleStatus.ACTIVE);
                                Map<UUID, Product3DModel> publishedModels = products.isEmpty() ? Map.of()
                                        : modelRepository.findAllByProduct_IdInAndStatus(
                                                products.stream().map(MenuProduct::getId).toList(),
                                                Product3DModelStatus.PUBLISHED).stream()
                                        .collect(Collectors.toMap(
                                                model -> model.getProduct().getId(), Function.identity()));
                                List<PublicRestaurantMenuResponse.PublicProduct> publicProducts = products.stream()
                                        .map(product -> {
                                            Product3DModel model = publishedModels.get(product.getId());
                                            PublicRestaurantMenuResponse.Public3DModel publicModel = model == null ? null
                                                    : new PublicRestaurantMenuResponse.Public3DModel(
                                                            model.getPublicUrl(), model.getContentType(),
                                                            model.getWidthMm(), model.getHeightMm(), model.getDepthMm());
                                            return new PublicRestaurantMenuResponse.PublicProduct(
                                                    product.getId(), product.getName(), product.getDescription(),
                                                    product.getPriceInCents(), product.getAvailability(),
                                                    product.getDisplayOrder(), product.getIngredients(),
                                                    product.getAddons().stream()
                                                            .filter(MenuProductAddon::isActive)
                                                            .map(addon -> new PublicRestaurantMenuResponse.PublicAddon(
                                                                    addon.getName(), addon.getPriceDeltaInCents()))
                                                            .toList(),
                                                    product.getImage() == null ? null : product.getImage().getPublicUrl(),
                                                    publicModel);
                                        })
                                        .toList();
                                return new PublicRestaurantMenuResponse.PublicCategory(
                                        category.getId(), category.getName(), category.getDescription(),
                                        category.getDisplayOrder(), publicProducts);
                            })
                            .toList();
                    return new PublicRestaurantMenuResponse.PublicMenu(
                            menu.getId(), menu.getName(), menu.getDescription(), menu.getDisplayOrder(), categories);
                })
                .toList();

        String logoUrl = mediaRepository.findFirstByRestaurant_IdAndMediaType(restaurant.getId(), RestaurantMediaType.LOGO)
                .map(com.borae.identity.RestaurantMedia::getPublicUrl)
                .orElse(null);
        return PublicRestaurantMenuResponse.from(restaurant, menus, logoUrl);
    }
}
