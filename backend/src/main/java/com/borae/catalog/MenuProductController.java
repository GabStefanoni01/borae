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
@RequestMapping("/api/v1/restaurants/{restaurantId}/menus/{menuId}/products")
@Tag(name = "Produtos do cardápio", description = "Gestão de produtos, ingredientes, adicionais e disponibilidade")
public class MenuProductController {

    private final UserAccountRepository userRepository;
    private final MenuProductService productService;

    public MenuProductController(UserAccountRepository userRepository, MenuProductService productService) {
        this.userRepository = userRepository;
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Listar produtos do cardápio", description = "categoryId é opcional para filtrar por categoria.")
    public List<MenuProductResponse> list(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                          @RequestParam(required = false) UUID categoryId,
                                          Authentication authentication) {
        return productService.list(restaurantId, menuId, categoryId, currentUser(authentication));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar produto com ingredientes e adicionais")
    public MenuProductResponse create(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                      @Valid @RequestBody CreateMenuProductRequest request,
                                      Authentication authentication) {
        return productService.create(restaurantId, menuId, request, currentUser(authentication));
    }

    @PutMapping("/{productId}")
    @Operation(summary = "Editar produto, ingredientes e adicionais")
    public MenuProductResponse update(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                      @PathVariable UUID productId,
                                      @Valid @RequestBody UpdateMenuProductRequest request,
                                      Authentication authentication) {
        return productService.update(restaurantId, menuId, productId, request, currentUser(authentication));
    }

    @PatchMapping("/{productId}/availability")
    @Operation(summary = "Alterar disponibilidade operacional", description = "OWNER, MANAGER e STAFF podem alterar apenas a disponibilidade.")
    public MenuProductResponse availability(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                            @PathVariable UUID productId,
                                            @Valid @RequestBody ChangeProductAvailabilityRequest request,
                                            Authentication authentication) {
        return productService.changeAvailability(restaurantId, menuId, productId,
                request.availability(), currentUser(authentication));
    }

    @PostMapping("/{productId}/archive")
    @Operation(summary = "Arquivar produto")
    public MenuProductResponse archive(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                       @PathVariable UUID productId, Authentication authentication) {
        return productService.archive(restaurantId, menuId, productId, currentUser(authentication));
    }

    @PostMapping("/{productId}/restore")
    @Operation(summary = "Reativar produto arquivado")
    public MenuProductResponse restore(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                       @PathVariable UUID productId, Authentication authentication) {
        return productService.restore(restaurantId, menuId, productId, currentUser(authentication));
    }

    @PutMapping("/order")
    @Operation(summary = "Reordenar produtos de uma categoria")
    public List<MenuProductResponse> reorder(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                              @Valid @RequestBody ReorderMenuProductsRequest request,
                                              Authentication authentication) {
        return productService.reorder(restaurantId, menuId, request.categoryId(),
                request.productIds(), currentUser(authentication));
    }

    private UserAccount currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou usuário inativo."));
    }
}
