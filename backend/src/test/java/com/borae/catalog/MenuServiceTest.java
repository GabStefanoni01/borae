package com.borae.catalog;

import com.borae.identity.Restaurant;
import com.borae.identity.RestaurantMembership;
import com.borae.identity.RestaurantMembershipRepository;
import com.borae.identity.RestaurantRepository;
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
class MenuServiceTest {

    @Mock private RestaurantRepository restaurantRepository;
    @Mock private RestaurantMembershipRepository membershipRepository;
    @Mock private MenuRepository menuRepository;
    @Mock private MenuCategoryRepository categoryRepository;

    @InjectMocks private MenuService menuService;

    private UserAccount owner;
    private Restaurant restaurant;
    private UUID restaurantId;

    @BeforeEach
    void setUp() {
        owner = new UserAccount("Owner", "owner@example.com", "hash");
        restaurant = new Restaurant("Boraê", "borae-12345678", null);
        restaurantId = restaurant.getId();
    }

    @Test
    void ownerCanCreateMenuAsDraft() {
        grantRole(RestaurantRole.OWNER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(menuRepository.save(any(Menu.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MenuResponse response = menuService.createMenu(restaurantId,
                new CreateMenuRequest(" Almoço ", " Menu executivo ", 0), owner);

        assertEquals("Almoço", response.name());
        assertEquals("Menu executivo", response.description());
        assertEquals(MenuStatus.DRAFT, response.status());
    }

    @Test
    void staffCannotCreateMenu() {
        grantRole(RestaurantRole.STAFF);

        assertThrows(ResponseStatusException.class, () -> menuService.createMenu(restaurantId,
                new CreateMenuRequest("Almoço", null, 0), owner));
        verify(restaurantRepository, never()).findById(restaurantId);
    }

    @Test
    void userWithoutMembershipCannotListMenus() {
        when(membershipRepository.findByUser_IdAndRestaurant_IdAndActiveTrue(owner.getId(), restaurantId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> menuService.listMenus(restaurantId, owner));
        verify(menuRepository, never()).findAllByRestaurant_IdOrderByDisplayOrderAscNameAsc(restaurantId);
    }

    @Test
    void menuCanBePublishedAndThenUnpublished() {
        grantRole(RestaurantRole.MANAGER);
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));

        assertEquals(MenuStatus.PUBLISHED, menuService.publishMenu(restaurantId, menu.getId(), owner).status());
        assertEquals(MenuStatus.DRAFT, menuService.unpublishMenu(restaurantId, menu.getId(), owner).status());
    }

    @Test
    void archivedMenuCannotBePublishedAgain() {
        grantRole(RestaurantRole.OWNER);
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        menu.archive();
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));

        assertThrows(ResponseStatusException.class,
                () -> menuService.publishMenu(restaurantId, menu.getId(), owner));
    }

    @Test
    void categoryMustBelongToMenuInRoute() {
        grantRole(RestaurantRole.OWNER);
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        UUID anotherMenuId = UUID.randomUUID();
        MenuCategory category = new MenuCategory(menu, "Pratos", null, 0);
        when(menuRepository.findByIdAndRestaurant_Id(anotherMenuId, restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findByIdAndMenu_Id(category.getId(), anotherMenuId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> menuService.updateCategory(restaurantId,
                anotherMenuId, category.getId(),
                new UpdateMenuCategoryRequest("Pratos", null, 0, true), owner));
    }

    @Test
    void categoryOrderMustContainEveryCategoryExactlyOnce() {
        grantRole(RestaurantRole.MANAGER);
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        MenuCategory first = new MenuCategory(menu, "Pratos", null, 0);
        MenuCategory second = new MenuCategory(menu, "Bebidas", null, 1);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findAllByMenu_IdOrderByDisplayOrderAscNameAsc(menu.getId()))
                .thenReturn(List.of(first, second));

        assertThrows(ResponseStatusException.class,
                () -> menuService.reorderCategories(restaurantId, menu.getId(), List.of(first.getId()), owner));
    }

    private void grantRole(RestaurantRole role) {
        RestaurantMembership membership = new RestaurantMembership(owner, restaurant, role);
        when(membershipRepository.findByUser_IdAndRestaurant_IdAndActiveTrue(owner.getId(), restaurantId))
                .thenReturn(Optional.of(membership));
    }
}
