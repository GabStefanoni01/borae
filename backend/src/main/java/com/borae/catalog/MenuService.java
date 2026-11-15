package com.borae.catalog;

import com.borae.identity.Restaurant;
import com.borae.identity.RestaurantMembership;
import com.borae.identity.RestaurantMembershipRepository;
import com.borae.identity.RestaurantRepository;
import com.borae.identity.RestaurantRole;
import com.borae.identity.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class MenuService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMembershipRepository membershipRepository;
    private final MenuRepository menuRepository;
    private final MenuCategoryRepository categoryRepository;

    public MenuService(RestaurantRepository restaurantRepository,
                       RestaurantMembershipRepository membershipRepository,
                       MenuRepository menuRepository,
                       MenuCategoryRepository categoryRepository) {
        this.restaurantRepository = restaurantRepository;
        this.membershipRepository = membershipRepository;
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuResponse> listMenus(UUID restaurantId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        return menuRepository.findAllByRestaurant_IdOrderByDisplayOrderAscNameAsc(restaurantId)
                .stream().map(MenuResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MenuResponse getMenu(UUID restaurantId, UUID menuId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        return MenuResponse.from(findMenu(restaurantId, menuId));
    }

    @Transactional
    public MenuResponse createMenu(UUID restaurantId, CreateMenuRequest request, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        Restaurant restaurant = findActiveRestaurant(restaurantId);
        Menu menu = menuRepository.save(new Menu(restaurant, normalizeRequired(request.name()),
                optionalText(request.description()), request.displayOrder()));
        return MenuResponse.from(menu);
    }

    @Transactional
    public MenuResponse updateMenu(UUID restaurantId, UUID menuId,
                                   UpdateMenuRequest request, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        Menu menu = findMenu(restaurantId, menuId);
        try {
            menu.update(normalizeRequired(request.name()), optionalText(request.description()),
                    request.displayOrder() == null ? menu.getDisplayOrder() : request.displayOrder());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return MenuResponse.from(menu);
    }

    @Transactional
    public MenuResponse publishMenu(UUID restaurantId, UUID menuId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        Menu menu = findMenu(restaurantId, menuId);
        try {
            menu.publish();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return MenuResponse.from(menu);
    }

    @Transactional
    public MenuResponse unpublishMenu(UUID restaurantId, UUID menuId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        Menu menu = findMenu(restaurantId, menuId);
        try {
            menu.unpublish();
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return MenuResponse.from(menu);
    }

    @Transactional
    public MenuResponse archiveMenu(UUID restaurantId, UUID menuId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        Menu menu = findMenu(restaurantId, menuId);
        menu.archive();
        return MenuResponse.from(menu);
    }

    @Transactional(readOnly = true)
    public List<MenuCategoryResponse> listCategories(UUID restaurantId, UUID menuId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findMenu(restaurantId, menuId);
        return categoryRepository.findAllByMenu_IdOrderByDisplayOrderAscNameAsc(menuId)
                .stream().map(MenuCategoryResponse::from).toList();
    }

    @Transactional
    public MenuCategoryResponse createCategory(UUID restaurantId, UUID menuId,
                                               CreateMenuCategoryRequest request, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        Menu menu = findEditableMenu(restaurantId, menuId);
        MenuCategory category = categoryRepository.save(new MenuCategory(menu,
                normalizeRequired(request.name()), optionalText(request.description()), request.displayOrder()));
        return MenuCategoryResponse.from(category);
    }

    @Transactional
    public MenuCategoryResponse updateCategory(UUID restaurantId, UUID menuId, UUID categoryId,
                                               UpdateMenuCategoryRequest request, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findEditableMenu(restaurantId, menuId);
        MenuCategory category = categoryRepository.findByIdAndMenu_Id(categoryId, menuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada."));
        category.update(normalizeRequired(request.name()), optionalText(request.description()),
                request.displayOrder(), request.active());
        return MenuCategoryResponse.from(category);
    }

    @Transactional
    public List<MenuCategoryResponse> reorderCategories(UUID restaurantId, UUID menuId,
                                                        List<UUID> categoryIds, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findEditableMenu(restaurantId, menuId);
        if (categoryIds == null || categoryIds.stream().anyMatch(id -> id == null)
                || new HashSet<>(categoryIds).size() != categoryIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe cada categoria uma única vez para definir a ordenação.");
        }

        List<MenuCategory> categories = categoryRepository.findAllByMenu_IdOrderByDisplayOrderAscNameAsc(menuId);
        Set<UUID> existingIds = categories.stream().map(MenuCategory::getId).collect(java.util.stream.Collectors.toSet());
        if (categoryIds.size() != existingIds.size() || !existingIds.equals(new HashSet<>(categoryIds))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A ordenação deve conter todas as categorias deste cardápio e nenhuma categoria externa.");
        }

        java.util.Map<UUID, MenuCategory> byId = categories.stream()
                .collect(java.util.stream.Collectors.toMap(MenuCategory::getId, category -> category));
        for (int position = 0; position < categoryIds.size(); position++) {
            MenuCategory category = byId.get(categoryIds.get(position));
            category.update(category.getName(), category.getDescription(), position, category.isActive());
        }
        return categories.stream().sorted(java.util.Comparator.comparingInt(MenuCategory::getDisplayOrder))
                .map(MenuCategoryResponse::from).toList();
    }

    private Menu findMenu(UUID restaurantId, UUID menuId) {
        return menuRepository.findByIdAndRestaurant_Id(menuId, restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cardápio não encontrado."));
    }

    private Menu findEditableMenu(UUID restaurantId, UUID menuId) {
        Menu menu = findMenu(restaurantId, menuId);
        if (menu.getStatus() == MenuStatus.ARCHIVED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cardápio arquivado não pode ser alterado.");
        }
        return menu;
    }

    private Restaurant findActiveRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .filter(Restaurant::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante não encontrado."));
    }

    private void requireManagerAccess(UUID restaurantId, UserAccount actor) {
        RestaurantMembership membership = membershipRepository
                .findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)
                .filter(item -> item.getRestaurant().isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você não tem acesso a este restaurante."));
        if (membership.getRole() != RestaurantRole.OWNER && membership.getRole() != RestaurantRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu papel não permite administrar cardápios e categorias.");
        }
    }

    private String normalizeRequired(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome é obrigatório.");
        }
        return value.trim();
    }

    private String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
