package com.borae.catalog;

import com.borae.identity.RestaurantMembership;
import com.borae.identity.RestaurantMembershipRepository;
import com.borae.identity.RestaurantRole;
import com.borae.identity.UserAccount;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MenuProductService {

    private final RestaurantMembershipRepository membershipRepository;
    private final MenuRepository menuRepository;
    private final MenuCategoryRepository categoryRepository;
    private final MenuProductRepository productRepository;

    public MenuProductService(RestaurantMembershipRepository membershipRepository,
                              MenuRepository menuRepository,
                              MenuCategoryRepository categoryRepository,
                              MenuProductRepository productRepository) {
        this.membershipRepository = membershipRepository;
        this.menuRepository = menuRepository;
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<MenuProductResponse> list(UUID restaurantId, UUID menuId, UUID categoryId, UserAccount actor) {
        requireMembership(restaurantId, actor);
        findMenu(restaurantId, menuId);
        List<MenuProduct> products;
        if (categoryId == null) {
            products = productRepository.findAllByMenuIdOrderByCategoryAndDisplayOrder(menuId);
        } else {
            findCategory(menuId, categoryId);
            products = productRepository.findAllByCategory_IdOrderByDisplayOrderAscNameAsc(categoryId);
        }
        return products.stream().map(MenuProductResponse::from).toList();
    }

    @Transactional
    public MenuProductResponse create(UUID restaurantId, UUID menuId,
                                      CreateMenuProductRequest request, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        Menu menu = findEditableMenu(restaurantId, menuId);
        MenuCategory category = findCategory(menu.getId(), request.categoryId());
        validateProductInput(request.name(), request.priceInCents(), request.displayOrder(),
                request.ingredients(), request.addons());

        MenuProduct product = new MenuProduct(category, normalizeRequired(request.name()),
                optionalText(request.description()), request.priceInCents(), request.displayOrder(),
                normalizeIngredients(request.ingredients()), normalizeAddons(request.addons()));
        return MenuProductResponse.from(productRepository.save(product));
    }

    @Transactional
    public MenuProductResponse update(UUID restaurantId, UUID menuId, UUID productId,
                                      UpdateMenuProductRequest request, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findEditableMenu(restaurantId, menuId);
        MenuProduct product = findProduct(menuId, productId);
        validateProductInput(request.name(), request.priceInCents(), request.displayOrder(),
                request.ingredients(), request.addons());
        MenuCategory category = findCategory(menuId, request.categoryId());
        try {
            product.update(category, normalizeRequired(request.name()), optionalText(request.description()),
                    request.priceInCents(), request.displayOrder(), normalizeIngredients(request.ingredients()),
                    normalizeAddons(request.addons()));
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return MenuProductResponse.from(product);
    }

    @Transactional
    public MenuProductResponse changeAvailability(UUID restaurantId, UUID menuId, UUID productId,
                                                   ProductAvailability availability, UserAccount actor) {
        requireMembership(restaurantId, actor);
        findEditableMenu(restaurantId, menuId);
        if (availability == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a disponibilidade do produto.");
        }
        MenuProduct product = findProduct(menuId, productId);
        try {
            product.changeAvailability(availability);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
        return MenuProductResponse.from(product);
    }

    @Transactional
    public MenuProductResponse archive(UUID restaurantId, UUID menuId, UUID productId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findEditableMenu(restaurantId, menuId);
        MenuProduct product = findProduct(menuId, productId);
        product.archive();
        return MenuProductResponse.from(product);
    }

    @Transactional
    public MenuProductResponse restore(UUID restaurantId, UUID menuId, UUID productId, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findEditableMenu(restaurantId, menuId);
        MenuProduct product = findProduct(menuId, productId);
        product.restore();
        return MenuProductResponse.from(product);
    }

    @Transactional
    public List<MenuProductResponse> reorder(UUID restaurantId, UUID menuId, UUID categoryId,
                                             List<UUID> productIds, UserAccount actor) {
        requireManagerAccess(restaurantId, actor);
        findEditableMenu(restaurantId, menuId);
        findCategory(menuId, categoryId);
        if (productIds == null || productIds.isEmpty()
                || productIds.stream().anyMatch(id -> id == null)
                || new HashSet<>(productIds).size() != productIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe cada produto uma única vez para definir a ordenação.");
        }
        List<MenuProduct> products = productRepository.findAllByCategory_IdOrderByDisplayOrderAscNameAsc(categoryId);
        Set<UUID> existingIds = products.stream().map(MenuProduct::getId).collect(Collectors.toSet());
        if (productIds.size() != existingIds.size() || !existingIds.equals(new HashSet<>(productIds))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A ordenação deve conter todos os produtos desta categoria e nenhum produto externo.");
        }
        Map<UUID, MenuProduct> byId = products.stream()
                .collect(Collectors.toMap(MenuProduct::getId, Function.identity()));
        for (int position = 0; position < productIds.size(); position++) {
            byId.get(productIds.get(position)).changeDisplayOrder(position);
        }
        return products.stream().sorted(java.util.Comparator.comparingInt(MenuProduct::getDisplayOrder))
                .map(MenuProductResponse::from).toList();
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

    private MenuCategory findCategory(UUID menuId, UUID categoryId) {
        return categoryRepository.findByIdAndMenu_Id(categoryId, menuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Categoria não encontrada neste cardápio."));
    }

    private MenuProduct findProduct(UUID menuId, UUID productId) {
        return productRepository.findByIdAndCategory_Menu_Id(productId, menuId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Produto não encontrado neste cardápio."));
    }

    private RestaurantMembership requireMembership(UUID restaurantId, UserAccount actor) {
        return membershipRepository
                .findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)
                .filter(item -> item.getRestaurant().isActive())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você não tem acesso a este restaurante."));
    }

    private void requireManagerAccess(UUID restaurantId, UserAccount actor) {
        RestaurantMembership membership = requireMembership(restaurantId, actor);
        if (membership.getRole() != RestaurantRole.OWNER && membership.getRole() != RestaurantRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seu papel não permite administrar produtos.");
        }
    }

    private void validateProductInput(String name, Long priceInCents, Integer displayOrder,
                                      List<String> ingredients, List<ProductAddonRequest> addons) {
        normalizeRequired(name);
        if (priceInCents == null || priceInCents < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O preço não pode ser negativo.");
        }
        if (displayOrder == null || displayOrder < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A ordem de exibição deve ser zero ou maior.");
        }
        normalizeIngredients(ingredients);
        normalizeAddons(addons);
    }

    private List<String> normalizeIngredients(List<String> ingredients) {
        if (ingredients == null || ingredients.size() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe até 40 ingredientes.");
        }
        List<String> normalized = ingredients.stream().map(this::normalizeRequired).toList();
        ensureUniqueNames(normalized, "Não repita ingredientes no mesmo produto.");
        return normalized;
    }

    private List<MenuProductAddon> normalizeAddons(List<ProductAddonRequest> addons) {
        if (addons == null || addons.size() > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe até 30 adicionais.");
        }
        List<String> names = addons.stream().map(addon -> normalizeRequired(addon.name())).toList();
        ensureUniqueNames(names, "Não repita adicionais no mesmo produto.");
        return java.util.stream.IntStream.range(0, addons.size())
                .mapToObj(index -> {
                    ProductAddonRequest addon = addons.get(index);
                    if (addon.priceDeltaInCents() == null || addon.priceDeltaInCents() < 0 || addon.active() == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Cada adicional precisa de preço não negativo e estado ativo/inativo.");
                    }
                    return new MenuProductAddon(names.get(index), addon.priceDeltaInCents(), addon.active());
                }).toList();
    }

    private void ensureUniqueNames(List<String> names, String message) {
        Set<String> normalized = names.stream()
                .map(value -> Normalizer.normalize(value, Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "").replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (normalized.size() != names.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
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
