package com.borae.catalog;

import com.borae.identity.UserAccount;
import com.borae.identity.UserAccountRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/menus/{menuId}/products/{productId}/model-3d/generations")
@Tag(name = "Geração 3D do produto", description = "Solicitação e acompanhamento de geração de modelos 3D")
public class Product3DGenerationJobController {

    private final UserAccountRepository userRepository;
    private final Product3DGenerationJobService jobService;

    public Product3DGenerationJobController(
            UserAccountRepository userRepository,
            Product3DGenerationJobService jobService
    ) {
        this.userRepository = userRepository;
        this.jobService = jobService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Solicitar geração 3D a partir da foto atual do produto",
            description = "Cria um job durável na fila. A execução pelo engine será ativada em uma etapa posterior.")
    public Product3DGenerationJobResponse create(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID productId,
            Authentication authentication
    ) {
        return jobService.create(restaurantId, menuId, productId, currentUser(authentication));
    }

    @GetMapping("/{jobId}")
    @Operation(summary = "Consultar o estado de uma geração 3D")
    public Product3DGenerationJobResponse get(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID productId,
            @PathVariable UUID jobId,
            Authentication authentication
    ) {
        return jobService.get(restaurantId, menuId, productId, jobId, currentUser(authentication));
    }

    @DeleteMapping("/{jobId}")
    @Operation(summary = "Cancelar uma geração 3D ainda na fila")
    public Product3DGenerationJobResponse cancel(
            @PathVariable UUID restaurantId,
            @PathVariable UUID menuId,
            @PathVariable UUID productId,
            @PathVariable UUID jobId,
            Authentication authentication
    ) {
        return jobService.cancel(restaurantId, menuId, productId, jobId, currentUser(authentication));
    }

    private UserAccount currentUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida.");
        }
        return userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou usuário inativo."));
    }
}
