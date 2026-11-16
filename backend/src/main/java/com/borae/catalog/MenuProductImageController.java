package com.borae.catalog;

import com.borae.identity.UserAccount;
import com.borae.identity.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/image")
@Tag(name = "Imagem do produto", description = "Upload e remoção da imagem de um produto do cardápio")
public class MenuProductImageController {

    private final UserAccountRepository userRepository;
    private final MenuProductImageService imageService;

    public MenuProductImageController(UserAccountRepository userRepository, MenuProductImageService imageService) {
        this.userRepository = userRepository;
        this.imageService = imageService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar ou substituir imagem do produto",
            description = "Aceita JPEG, PNG e WebP com até 10 MB. A imagem é processada no backend e armazenada no ImageKit.")
    public MenuProductResponse upload(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                                      @PathVariable UUID productId, @RequestParam("file") MultipartFile file,
                                      Authentication authentication) {
        return imageService.upload(restaurantId, menuId, productId, file, currentUser(authentication));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remover imagem do produto")
    public void delete(@PathVariable UUID restaurantId, @PathVariable UUID menuId,
                       @PathVariable UUID productId, Authentication authentication) {
        imageService.delete(restaurantId, menuId, productId, currentUser(authentication));
    }

    private UserAccount currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou usuário inativo."));
    }
}
