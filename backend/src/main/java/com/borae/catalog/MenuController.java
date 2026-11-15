package com.borae.catalog;

import com.borae.identity.UserAccount;
import com.borae.identity.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menus")
@Tag(name = "Cardápios", description = "Gestão de cardápios e categorias do restaurante")
public class MenuController {

    private final UserAccountRepository userRepository;
    private final MenuService menuService;

    public MenuController(UserAccountRepository userRepository, MenuService menuService) {
        this.userRepository = userRepository;
        this.menuService = menuService;
    }

    @GetMapping
    @Operation(summary = "Listar cardápios do restaurante")
    public List<MenuResponse> list(@PathVariable UUID restaurantId, Authentication authentication) {
        return menuService.listMenus(restaurantId, currentUser(authentication));
    }

    @GetMapping("/{menuId}")
    @Operation(summary = "Consultar cardápio")
    public MenuResponse get(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                            Authentication authentication) {
        return menuService.getMenu(restaurantId, menuId, currentUser(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar cardápio em rascunho")
    public MenuResponse create(@PathVariable UUID restaurantId,
                               @Valid @RequestBody CreateMenuRequest request,
                               Authentication authentication) {
        return menuService.createMenu(restaurantId, request, currentUser(authentication));
    }

    @PutMapping("/{menuId}")
    @Operation(summary = "Atualizar cardápio")
    public MenuResponse update(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                               @Valid @RequestBody UpdateMenuRequest request,
                               Authentication authentication) {
        return menuService.updateMenu(restaurantId, menuId, request, currentUser(authentication));
    }

    @PostMapping("/{menuId}/publish")
    @Operation(summary = "Publicar cardápio")
    public MenuResponse publish(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                Authentication authentication) {
        return menuService.publishMenu(restaurantId, menuId, currentUser(authentication));
    }

    @PostMapping("/{menuId}/unpublish")
    @Operation(summary = "Despublicar cardápio")
    public MenuResponse unpublish(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                  Authentication authentication) {
        return menuService.unpublishMenu(restaurantId, menuId, currentUser(authentication));
    }

    @PostMapping("/{menuId}/archive")
    @Operation(summary = "Arquivar cardápio")
    public MenuResponse archive(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                Authentication authentication) {
        return menuService.archiveMenu(restaurantId, menuId, currentUser(authentication));
    }

    @GetMapping("/{menuId}/categories")
    @Operation(summary = "Listar categorias do cardápio")
    public List<MenuCategoryResponse> listCategories(@PathVariable UUID restaurantId,
                                                     @PathVariable UUID menuId,
                                                     Authentication authentication) {
        return menuService.listCategories(restaurantId, menuId, currentUser(authentication));
    }

    @PostMapping("/{menuId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar categoria no cardápio")
    public MenuCategoryResponse createCategory(@PathVariable UUID restaurantId,
                                               @PathVariable UUID menuId,
                                               @Valid @RequestBody CreateMenuCategoryRequest request,
                                               Authentication authentication) {
        return menuService.createCategory(restaurantId, menuId, request, currentUser(authentication));
    }

    @PutMapping("/{menuId}/categories/{categoryId}")
    @Operation(summary = "Atualizar categoria")
    public MenuCategoryResponse updateCategory(@PathVariable UUID restaurantId,
                                               @PathVariable UUID menuId,
                                               @PathVariable UUID categoryId,
                                               @Valid @RequestBody UpdateMenuCategoryRequest request,
                                               Authentication authentication) {
        return menuService.updateCategory(restaurantId, menuId, categoryId, request, currentUser(authentication));
    }

    @PutMapping("/{menuId}/categories/order")
    @Operation(summary = "Reordenar categorias")
    public List<MenuCategoryResponse> reorderCategories(@PathVariable UUID restaurantId,
                                                        @PathVariable UUID menuId,
                                                        @RequestBody List<UUID> categoryIds,
                                                        Authentication authentication) {
        return menuService.reorderCategories(restaurantId, menuId, categoryIds, currentUser(authentication));
    }

    private UserAccount currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sessão inválida ou usuário inativo."));
    }
}
