package com.borae.catalog;

import com.borae.identity.Restaurant;
import com.borae.identity.RestaurantPublicationStatus;
import com.borae.identity.RestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicRestaurantMenuServiceTest {

    @Mock private RestaurantRepository restaurantRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private MenuProductRepository productRepository;

    private PublicRestaurantMenuService service;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        service = new PublicRestaurantMenuService(
                restaurantRepository, menuRepository, categoryRepository, productRepository);
        restaurant = new Restaurant("Restaurante Teste", "restaurante-teste", "Descrição pública");
        ReflectionTestUtils.setField(restaurant, "publicationStatus", RestaurantPublicationStatus.PUBLISHED);
    }

    @Test
    void returnsOnlyPublishedMenusActiveCategoriesAndNonArchivedProducts() {
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        menu.publish();
        MenuCategory category = new MenuCategory(menu, "Pratos", null, 0);
        MenuProduct product = new MenuProduct(category, "Prato do dia", "Descrição", 2590L, 0,
                List.of("Arroz", "Feijão"),
                List.of(new MenuProductAddon("Suco", 500L, true),
                        new MenuProductAddon("Extra inativo", 300L, false)));

        when(restaurantRepository.findBySlugAndActiveTrueAndPublicationStatus(
                "restaurante-teste", RestaurantPublicationStatus.PUBLISHED)).thenReturn(Optional.of(restaurant));
        when(menuRepository.findAllByRestaurant_IdAndStatusOrderByDisplayOrderAscNameAsc(
                restaurant.getId(), MenuStatus.PUBLISHED)).thenReturn(List.of(menu));
        when(categoryRepository.findAllByMenu_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(menu.getId()))
                .thenReturn(List.of(category));
        when(productRepository.findAllByCategory_IdAndLifecycleStatusOrderByDisplayOrderAscNameAsc(
                category.getId(), ProductLifecycleStatus.ACTIVE)).thenReturn(List.of(product));

        PublicRestaurantMenuResponse response = service.getBySlug("restaurante-teste");

        assertEquals("Restaurante Teste", response.name());
        assertEquals(1, response.menus().size());
        assertEquals(1, response.menus().get(0).categories().size());
        assertEquals(1, response.menus().get(0).categories().get(0).products().size());
        PublicRestaurantMenuResponse.PublicProduct publicProduct =
                response.menus().get(0).categories().get(0).products().get(0);
        assertEquals(2590L, publicProduct.priceInCents());
        assertEquals(ProductAvailability.AVAILABLE, publicProduct.availability());
        assertEquals(List.of("Arroz", "Feijão"), publicProduct.ingredients());
        assertEquals(1, publicProduct.addons().size());
        assertEquals("Suco", publicProduct.addons().get(0).name());
    }

    @Test
    void unavailableOrUnpublishedRestaurantIsNotFoundAndNoCatalogIsRead() {
        when(restaurantRepository.findBySlugAndActiveTrueAndPublicationStatus(
                "rascunho", RestaurantPublicationStatus.PUBLISHED)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.getBySlug("rascunho"));

        assertEquals(404, exception.getStatusCode().value());
        verifyNoInteractions(menuRepository, categoryRepository, productRepository);
    }

    @Test
    void doesNotQueryProductsForMenusWithoutActiveCategories() {
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        menu.publish();
        when(restaurantRepository.findBySlugAndActiveTrueAndPublicationStatus(
                "restaurante-teste", RestaurantPublicationStatus.PUBLISHED)).thenReturn(Optional.of(restaurant));
        when(menuRepository.findAllByRestaurant_IdAndStatusOrderByDisplayOrderAscNameAsc(
                restaurant.getId(), MenuStatus.PUBLISHED)).thenReturn(List.of(menu));
        when(categoryRepository.findAllByMenu_IdAndActiveTrueOrderByDisplayOrderAscNameAsc(menu.getId()))
                .thenReturn(List.of());

        PublicRestaurantMenuResponse response = service.getBySlug("restaurante-teste");

        assertEquals(1, response.menus().size());
        assertEquals(List.of(), response.menus().get(0).categories());
        verifyNoInteractions(productRepository);
    }
}
