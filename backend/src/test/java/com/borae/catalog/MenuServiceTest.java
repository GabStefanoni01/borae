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
    @Mock private MenuProductRepository productRepository;

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
    void staffCanReadMenusAndCategoriesButCannotAdministerThem() {
        grantRole(RestaurantRole.STAFF);
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        MenuCategory category = new MenuCategory(menu, "Pratos", null, 0);
        when(menuRepository.findAllByRestaurant_IdOrderByDisplayOrderAscNameAsc(restaurantId))
                .thenReturn(List.of(menu));
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findAllByMenu_IdOrderByDisplayOrderAscNameAsc(menu.getId()))
                .thenReturn(List.of(category));

        assertEquals(1, menuService.listMenus(restaurantId, owner).size());
        assertEquals(menu.getId(), menuService.getMenu(restaurantId, menu.getId(), owner).id());
        assertEquals(1, menuService.listCategories(restaurantId, menu.getId(), owner).size());

        assertThrows(ResponseStatusException.class, () -> menuService.createMenu(restaurantId,
                new CreateMenuRequest("Jantar", null, 1), owner));
        verify(menuRepository, never()).save(any(Menu.class));
    }

    @Test
    void menuCanBePublishedAndThenUnpublished() {
        grantRole(RestaurantRole.MANAGER);
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(productRepository.countByCategory_Menu_IdAndCategory_ActiveTrueAndLifecycleStatusNot(
                menu.getId(), ProductLifecycleStatus.ARCHIVED)).thenReturn(1L);

        assertEquals(MenuStatus.PUBLISHED, menuService.publishMenu(restaurantId, menu.getId(), owner).status());
        assertEquals(MenuStatus.DRAFT, menuService.unpublishMenu(restaurantId, menu.getId(), owner).status());
    }


    @Test
    void menuWithoutActiveCategoryAndNonArchivedProductCannotBePublished() {
        grantRole(RestaurantRole.OWNER);
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(productRepository.countByCategory_Menu_IdAndCategory_ActiveTrueAndLifecycleStatusNot(
                menu.getId(), ProductLifecycleStatus.ARCHIVED)).thenReturn(0L);

        assertThrows(ResponseStatusException.class,
                () -> menuService.publishMenu(restaurantId, menu.getId(), owner));
        assertEquals(MenuStatus.DRAFT, menu.getStatus());
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
    void categoryNameMustBeUniqueIgnoringCaseAndAccentsWhenCreating() {
        grantRole(RestaurantRole.OWNER);
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        MenuCategory existing = new MenuCategory(menu, "Bebidas", null, 0);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findAllByMenu_IdOrderByDisplayOrderAscNameAsc(menu.getId()))
                .thenReturn(List.of(existing));

        assertThrows(ResponseStatusException.class, () -> menuService.createCategory(restaurantId, menu.getId(),
                new CreateMenuCategoryRequest("BEBÍDAS", null, 1), owner));
        verify(categoryRepository, never()).save(any(MenuCategory.class));
    }

    @Test
    void categoryNameMustBeUniqueWhenUpdating() {
        grantRole(RestaurantRole.MANAGER);
        Menu menu = new Menu(restaurant, "Almoço", null, 0);
        MenuCategory existing = new MenuCategory(menu, "Bebidas", null, 0);
        MenuCategory target = new MenuCategory(menu, "Lanches", null, 1);
        when(menuRepository.findByIdAndRestaurant_Id(menu.getId(), restaurantId)).thenReturn(Optional.of(menu));
        when(categoryRepository.findByIdAndMenu_Id(target.getId(), menu.getId())).thenReturn(Optional.of(target));
        when(categoryRepository.findAllByMenu_IdOrderByDisplayOrderAscNameAsc(menu.getId()))
                .thenReturn(List.of(existing, target));

        assertThrows(ResponseStatusException.class, () -> menuService.updateCategory(restaurantId, menu.getId(),
                target.getId(), new UpdateMenuCategoryRequest("BEBÍDAS", null, 1, true), owner));
        assertEquals("Lanches", target.getName());
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
