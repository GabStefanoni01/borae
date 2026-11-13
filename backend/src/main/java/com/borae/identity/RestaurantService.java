package com.borae.identity;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        String description = request.description() == null || request.description().isBlank()
                ? null
                : request.description().trim();

        Restaurant restaurant = restaurantRepository.save(
                new Restaurant(name, createSlug(name), description)
        );
        RestaurantMembership membership = membershipRepository.save(
                new RestaurantMembership(owner, restaurant, RestaurantRole.OWNER)
        );

        return UserRestaurantAccessResponse.from(membership);
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
