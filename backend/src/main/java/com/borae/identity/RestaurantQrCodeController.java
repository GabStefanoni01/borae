package com.borae.identity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/restaurants")
@Tag(name = "QR Code", description = "Geração do QR Code principal do restaurante")
public class RestaurantQrCodeController {

    private final UserAccountRepository userRepository;
    private final RestaurantQrCodeService qrCodeService;

    public RestaurantQrCodeController(
            UserAccountRepository userRepository,
            RestaurantQrCodeService qrCodeService
    ) {
        this.userRepository = userRepository;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping(value = "/{restaurantId}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(
            summary = "Baixar QR Code do restaurante",
            description = "Gera um PNG que direciona para a URL pública do restaurante. Disponível para OWNER e MANAGER."
    )
    public ResponseEntity<byte[]> download(@PathVariable UUID restaurantId, Authentication authentication) {
        UserAccount actor = userRepository.findByEmail(authentication.getName())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Sessão inválida ou usuário inativo."));

        byte[] png = qrCodeService.generateRestaurantQrCode(restaurantId, actor);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"restaurant-" + restaurantId + "-qrcode.png\"")
                .body(png);
    }
}
