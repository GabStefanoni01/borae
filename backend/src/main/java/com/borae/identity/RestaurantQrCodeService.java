package com.borae.identity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RestaurantQrCodeService {

    private static final int IMAGE_SIZE = 640;

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMembershipRepository membershipRepository;
    private final String publicRestaurantUrlTemplate;

    public RestaurantQrCodeService(
            RestaurantRepository restaurantRepository,
            RestaurantMembershipRepository membershipRepository,
            @org.springframework.beans.factory.annotation.Value("${app.public-restaurant-url-template}") String publicRestaurantUrlTemplate
    ) {
        this.restaurantRepository = restaurantRepository;
        this.membershipRepository = membershipRepository;
        if (publicRestaurantUrlTemplate == null || !publicRestaurantUrlTemplate.contains("{slug}")) {
            throw new IllegalStateException("A configuração app.public-restaurant-url-template deve conter o placeholder {slug}.");
        }
        this.publicRestaurantUrlTemplate = publicRestaurantUrlTemplate;
    }

    @Transactional(readOnly = true)
    public byte[] generateRestaurantQrCode(UUID restaurantId, UserAccount actor) {
        RestaurantMembership membership = membershipRepository
                .findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Você não tem acesso a este restaurante."));

        if (membership.getRole() != RestaurantRole.OWNER && membership.getRole() != RestaurantRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Somente OWNER ou MANAGER pode gerar o QR Code do restaurante.");
        }

        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .filter(Restaurant::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante não encontrado."));

        String encodedSlug = UriUtils.encodePathSegment(restaurant.getSlug(), StandardCharsets.UTF_8);
        String publicUrl = publicRestaurantUrlTemplate.replace("{slug}", encodedSlug);
        URI uri;
        try {
            uri = URI.create(publicUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("A URL pública configurada para o QR Code é inválida.", exception);
        }
        if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalStateException("A URL pública do QR Code deve ser uma URL HTTP ou HTTPS absoluta.");
        }

        try {
            Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
            hints.put(EncodeHintType.MARGIN, 2);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            var matrix = new QRCodeWriter().encode(publicUrl, BarcodeFormat.QR_CODE, IMAGE_SIZE, IMAGE_SIZE, hints);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException exception) {
            throw new IllegalStateException("Não foi possível gerar o QR Code do restaurante.", exception);
        }
    }
}
