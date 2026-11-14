package com.borae.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants/{restaurantId}/media")
@Tag(name = "Mídia do restaurante", description = "Upload e gestão de logo e fotos do restaurante")
public class RestaurantMediaController {

    private final UserAccountRepository userRepository;
    private final RestaurantMediaService mediaService;

    public RestaurantMediaController(UserAccountRepository userRepository, RestaurantMediaService mediaService) {
        this.userRepository = userRepository;
        this.mediaService = mediaService;
    }

    @GetMapping
    @Operation(summary = "Listar imagens do restaurante")
    public List<RestaurantMediaResponse> list(@PathVariable UUID restaurantId, Authentication authentication) {
        return mediaService.list(restaurantId, currentUser(authentication));
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Enviar logo ou foto", description = "Aceita JPEG, PNG e WebP. Logo: até 5 MB. Foto: até 10 MB.")
    public RestaurantMediaResponse upload(
            @PathVariable UUID restaurantId,
            @RequestParam RestaurantMediaType type,
            @RequestParam("file") MultipartFile file,
            Authentication authentication
    ) {
        return mediaService.upload(restaurantId, type, file, currentUser(authentication));
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Excluir imagem do restaurante")
    public void delete(@PathVariable UUID restaurantId, @PathVariable UUID mediaId, Authentication authentication) {
        mediaService.delete(restaurantId, mediaId, currentUser(authentication));
    }

    private UserAccount currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Sessão inválida ou usuário inativo."));
    }
}
