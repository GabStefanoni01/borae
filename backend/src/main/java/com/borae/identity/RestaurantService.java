package com.borae.identity;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMembershipRepository membershipRepository;

    public RestaurantService(
            RestaurantRepository restaurantRepository,
            RestaurantMembershipRepository membershipRepository
    ) {
        this.restaurantRepository = restaurantRepository;
        this.membershipRepository = membershipRepository;
    }

    @Transactional
    public UserRestaurantAccessResponse create(CreateRestaurantRequest request, UserAccount owner) {
        String name = request.name().trim();
        String description = optionalText(request.description());

        Restaurant restaurant = restaurantRepository.save(
                new Restaurant(name, createSlug(name), description)
        );
        RestaurantMembership membership = membershipRepository.save(
                new RestaurantMembership(owner, restaurant, RestaurantRole.OWNER)
        );

        return UserRestaurantAccessResponse.from(membership);
    }

    @Transactional(readOnly = true)
    public RestaurantProfileResponse getProfile(UUID restaurantId, UserAccount actor) {
        requireProfilePermission(restaurantId, actor);
        Restaurant restaurant = findActiveRestaurant(restaurantId);
        return RestaurantProfileResponse.from(restaurant);
    }

    @Transactional
    public RestaurantProfileResponse updateProfile(
            UUID restaurantId,
            UpdateRestaurantProfileRequest request,
            UserAccount actor
    ) {
        requireProfilePermission(restaurantId, actor);
        Restaurant restaurant = findActiveRestaurant(restaurantId);

        String name = request.name() == null ? restaurant.getName() : request.name().trim();
        if (name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome do restaurante não pode ficar vazio.");
        }

        String slug = request.slug() == null ? restaurant.getSlug() : normalizeSlug(request.slug());
        if (!slug.equals(restaurant.getSlug())
                && restaurantRepository.existsBySlugAndIdNot(slug, restaurantId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este endereço público já está em uso.");
        }

        BigDecimal latitude = request.latitude() == null ? restaurant.getLatitude() : request.latitude();
        BigDecimal longitude = request.longitude() == null ? restaurant.getLongitude() : request.longitude();
        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Latitude e longitude devem ser informadas ou removidas em conjunto."
            );
        }

        restaurant.updateProfile(
                name,
                slug,
                request.description() == null ? restaurant.getDescription() : optionalText(request.description()),
                request.phone() == null ? restaurant.getPhone() : optionalText(request.phone()),
                request.whatsapp() == null ? restaurant.getWhatsapp() : optionalText(request.whatsapp()),
                request.instagram() == null ? restaurant.getInstagram() : optionalText(request.instagram()),
                request.website() == null ? restaurant.getWebsite() : optionalText(request.website()),
                request.address() == null ? restaurant.getAddress() : optionalText(request.address()),
                latitude,
                longitude,
                request.category() == null ? restaurant.getCategory() : optionalText(request.category())
        );
        return RestaurantProfileResponse.from(restaurant);
    }

    private String optionalText(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private Restaurant findActiveRestaurant(UUID restaurantId) {
        return restaurantRepository.findById(restaurantId)
                .filter(Restaurant::isActive)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Restaurante não encontrado."));
    }

    private void requireProfilePermission(UUID restaurantId, UserAccount actor) {
        RestaurantMembership membership = membershipRepository
                .findByUser_IdAndRestaurant_IdAndActiveTrue(actor.getId(), restaurantId)
                .filter(item -> item.getRestaurant().isActive())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Você não tem acesso a este restaurante."
                ));

        if (membership.getRole() != RestaurantRole.OWNER && membership.getRole() != RestaurantRole.MANAGER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel não permite administrar o perfil.");
        }
    }

    private String normalizeSlug(String value) {
        String normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");

        if (normalized.isBlank() || normalized.length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O endereço público informado é inválido.");
        }
        return normalized;
    }

    private String createSlug(String name) {
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (normalized.isBlank()) normalized = "restaurante";
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        int maxBaseLength = 120 - suffix.length() - 1;
        String base = normalized.substring(0, Math.min(normalized.length(), maxBaseLength));
        return base + "-" + suffix;
    }
}
