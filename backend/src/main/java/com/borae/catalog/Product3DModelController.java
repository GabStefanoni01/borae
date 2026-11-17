package com.borae.catalog;

import com.borae.identity.UserAccount;
import com.borae.identity.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/model-3d")
@Tag(name = "Modelo 3D do produto", description = "Upload, publicação e remoção de modelos GLB associados aos produtos")
public class Product3DModelController {

    private final UserAccountRepository userRepository;
    private final Product3DModelService modelService;

    public Product3DModelController(UserAccountRepository userRepository, Product3DModelService modelService) {
        this.userRepository = userRepository;
        this.modelService = modelService;
    }

    @GetMapping
    @Operation(summary = "Consultar o modelo 3D do produto")
    public Product3DModelResponse get(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                      @PathVariable UUID productId, Authentication authentication) {
        return modelService.get(restaurantId, menuId, productId, currentUser(authentication));
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar ou substituir o modelo 3D do produto",
            description = "Aceita GLB 2.0 até 10 MB. Dimensões físicas devem ser informadas em milímetros.")
    public Product3DModelResponse upload(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                         @PathVariable UUID productId,
                                         @RequestParam("file") MultipartFile file,
                                         @RequestParam @Positive int widthMm,
                                         @RequestParam @Positive int heightMm,
                                         @RequestParam @Positive int depthMm,
                                         Authentication authentication) {
        return modelService.upload(restaurantId, menuId, productId, file,
                widthMm, heightMm, depthMm, currentUser(authentication));
    }

    @PatchMapping("/publication")
    @Operation(summary = "Publicar ou despublicar o modelo 3D")
    public Product3DModelResponse publication(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                               @PathVariable UUID productId,
                                               @Valid @RequestBody UpdateProduct3DModelPublicationRequest request,
                                               Authentication authentication) {
        return modelService.updatePublication(restaurantId, menuId, productId,
                request.published(), currentUser(authentication));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover o modelo 3D do produto")
    public void delete(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                       @PathVariable UUID productId, Authentication authentication) {
        modelService.delete(restaurantId, menuId, productId, currentUser(authentication));
    }

    private UserAccount currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou usuário inativo."));
    }
}
