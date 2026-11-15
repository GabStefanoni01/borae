package com.borae.catalog;

import com.borae.identity.Restaurant;
import com.borae.identity.RestaurantMembership;
import com.borae.identity.RestaurantMembershipRepository;
import com.borae.identity.RestaurantRole;
import com.borae.identity.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuProductServiceTest {

    @Mock private RestaurantMembershipRepository membershipRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private MenuCategoryRepository categoryRepository;
    @Mock private MenuProductRepository productRepository;

    @InjectMocks private MenuProductService service;

    private UserAccount actor;
    private Restaurant restaurant;
    private UUID restaurantId;
    private Menu menu;
    private MenuCategory category;

    @BeforeEach
    void setUp() {
        actor = new UserAccount("Owner", "owner@example.com", "hash");
        restaurant = new Restaurant("Boraê", "borae-12345678", null);
        restaurantId = restaurant.getId();
        menu = new Menu(restaurant, "Almoço", null, 0);
        category = new MenuCategory(menu, "Pratos", null, 0);
    }

    @Test
    void ownerCanCreateProductWithIngredientsAndAddons() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findByIdAndMenu_Id(category.getId(), menu.getId())).thenReturn(Optional.of(category));
        when(productRepository.save(any(MenuProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuProductResponse response = service.create(restaurantId, menu.getId(),
                request(category.getId(), "Hambúrguer", 2590L), actor);

        assertEquals("Hambúrguer", response.name());
        assertEquals(2590L, response.priceInCents());
        assertEquals(List.of("Pão", "Carne"), response.ingredients());
        assertEquals("Cheddar", response.addons().get(0).name());
        assertEquals(300L, response.addons().get(0).priceDeltaInCents());
    }

    @Test
    void staffCannotEditProductContent() {
        grantRole(RestaurantRole.STAFF);
        assertThrows(ResponseStatusException.class, () -> service.create(restaurantId, menu.getId(),
                request(category.getId(), "Hambúrguer", 2590L), actor));
        verify(productRepository, never()).save(any(MenuProduct.class));
    }

    @Test
    void staffCanChangeAvailabilityOnlyForTheirRestaurant() {
        grantRole(RestaurantRole.STAFF);
        MenuProduct product = new MenuProduct(category, "Hambúrguer", null, 2590L, 0,
                List.of("Pão"), List.of());
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(productRepository.findByIdAndCategory_Menu_Id(product.getId(), menu.getId()))
                .thenReturn(Optional.of(product));

        MenuProductResponse response = service.changeAvailability(restaurantId, menu.getId(), product.getId(),
                ProductAvailability.UNAVAILABLE, actor);

        assertEquals(ProductAvailability.UNAVAILABLE, response.availability());
        assertEquals(2590L, response.priceInCents());
    }

    @Test
    void cannotChangeAvailabilityThroughAnotherRestaurantsMenu() {
        grantRole(RestaurantRole.STAFF);
        Restaurant anotherRestaurant = new Restaurant("Outro", "outro-12345678", null);
        Menu anotherMenu = new Menu(anotherRestaurant, "Jantar", null, 0);

        assertThrows(ResponseStatusException.class, () -> service.changeAvailability(restaurantId,
                anotherMenu.getId(), UUID.randomUUID(), ProductAvailability.UNAVAILABLE, actor));
        verify(productRepository, never()).findByIdAndCategory_Menu_Id(any(UUID.class), any(UUID.class));
    }

    @Test
    void categoryMustBelongToTheMenu() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findByIdAndMenu_Id(any(UUID.class), any(UUID.class))).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> service.create(restaurantId, menu.getId(),
                request(UUID.randomUUID(), "Hambúrguer", 2590L), actor));
    }

    @Test
    void negativeAddonPriceIsRejected() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findByIdAndMenu_Id(category.getId(), menu.getId())).thenReturn(Optional.of(category));
        CreateMenuProductRequest invalid = new CreateMenuProductRequest(category.getId(), "Hambúrguer", null,
                2590L, 0, List.of(), List.of(new ProductAddonRequest("Cheddar", -1L, true)));

        assertThrows(ResponseStatusException.class, () -> service.create(restaurantId, menu.getId(), invalid, actor));
        verify(productRepository, never()).save(any(MenuProduct.class));
    }

    @Test
    void duplicateIngredientsAreRejectedIgnoringCase() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findByIdAndMenu_Id(category.getId(), menu.getId())).thenReturn(Optional.of(category));
        CreateMenuProductRequest invalid = new CreateMenuProductRequest(category.getId(), "Hambúrguer", null,
                2590L, 0, List.of("Cebola", "cebola"), List.of());

        assertThrows(ResponseStatusException.class, () -> service.create(restaurantId, menu.getId(), invalid, actor));
    }

    @Test
    void archivedProductsCannotBeEdited() {
        grantRole(RestaurantRole.OWNER);
        MenuProduct product = new MenuProduct(category, "Hambúrguer", null, 2590L, 0, List.of(), List.of());
        product.archive();
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(productRepository.findByIdAndCategory_Menu_Id(product.getId(), menu.getId()))
                .thenReturn(Optional.of(product));
        when(categoryRepository.findByIdAndMenu_Id(category.getId(), menu.getId())).thenReturn(Optional.of(category));

        assertThrows(ResponseStatusException.class, () -> service.update(restaurantId, menu.getId(), product.getId(),
                new UpdateMenuProductRequest(category.getId(), "Novo nome", null, 100L, 0,
                        List.of(), List.of()), actor));
    }


    @Test
    void archivedProductsCannotChangeAvailability() {
        grantRole(RestaurantRole.STAFF);
        MenuProduct product = new MenuProduct(category, "Hambúrguer", null, 2590L, 0,
                List.of("Pão"), List.of());
        product.archive();
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(productRepository.findByIdAndCategory_Menu_Id(product.getId(), menu.getId()))
                .thenReturn(Optional.of(product));

        assertThrows(ResponseStatusException.class, () -> service.changeAvailability(restaurantId, menu.getId(),
                product.getId(), ProductAvailability.UNAVAILABLE, actor));
        assertEquals(ProductAvailability.AVAILABLE, product.getAvailability());
    }

    @Test
    void duplicateAddonNamesAreRejectedIgnoringRepeatedWhitespace() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findByIdAndMenu_Id(category.getId(), menu.getId())).thenReturn(Optional.of(category));
        CreateMenuProductRequest invalid = new CreateMenuProductRequest(category.getId(), "Hambúrguer", null,
                2590L, 0, List.of(), List.of(
                new ProductAddonRequest("Queijo extra", 300L, true),
                new ProductAddonRequest("QUEIJO   EXTRA", 300L, true)));

        assertThrows(ResponseStatusException.class, () -> service.create(restaurantId, menu.getId(), invalid, actor));
        verify(productRepository, never()).save(any(MenuProduct.class));
    }


    @Test
    void nullAddonEntriesAreRejectedAsBadRequest() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findByIdAndMenu_Id(category.getId(), menu.getId())).thenReturn(Optional.of(category));
        CreateMenuProductRequest invalid = new CreateMenuProductRequest(category.getId(), "Hambúrguer", null,
                2590L, 0, List.of(), java.util.Arrays.asList((ProductAddonRequest) null));

        assertThrows(ResponseStatusException.class, () -> service.create(restaurantId, menu.getId(), invalid, actor));
        verify(productRepository, never()).save(any(MenuProduct.class));
    }


    @Test
    void productTextFieldsAreTrimmedAndRepeatedWhitespaceIsCollapsed() {
        grantRole(RestaurantRole.OWNER);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findByIdAndMenu_Id(category.getId(), menu.getId())).thenReturn(Optional.of(category));
        when(productRepository.save(any(MenuProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));
        CreateMenuProductRequest input = new CreateMenuProductRequest(category.getId(), "  Hambúrguer   artesanal ",
                "  Feito   na casa ", 2590L, 0, List.of("  Cebola   roxa "),
                List.of(new ProductAddonRequest("  Queijo   extra ", 300L, true)));

        MenuProductResponse response = service.create(restaurantId, menu.getId(), input, actor);

        assertEquals("Hambúrguer artesanal", response.name());
        assertEquals("Feito na casa", response.description());
        assertEquals(List.of("Cebola roxa"), response.ingredients());
        assertEquals("Queijo extra", response.addons().get(0).name());
    }

    private CreateMenuProductRequest request(UUID categoryId, String name, long price) {
        return new CreateMenuProductRequest(categoryId, name, null, price, 0,
                List.of("Pão", "Carne"), List.of(new ProductAddonRequest("Cheddar", 300L, true)));
    }

    private void grantRole(RestaurantRole role) {
        RestaurantMembership membership = new RestaurantMembership(actor, restaurant, role);
        when(membershipRepository.findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId))
                .thenReturn(Optional.of(membership));
    }
}
