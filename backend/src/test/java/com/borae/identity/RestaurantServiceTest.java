package com.borae.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantMembershipRepository membershipRepository;

    @InjectMocks
    private RestaurantService restaurantService;

    private UserAccount owner;
    private Restaurant restaurant;
    private UUID restaurantId;

    @BeforeEach
    void setUp() {
        owner = new UserAccount("Owner", "owner@example.com", "hash");
        restaurant = new Restaurant("Boraê", "borae-12345678", "Descrição inicial");
        restaurantId = restaurant.getId();
    }

    @Test
    void ownerCanReadRestaurantProfile() {
        grantRole(RestaurantRole.OWNER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        RestaurantProfileResponse response = restaurantService.getProfile(restaurantId, owner);

        assertEquals("Boraê", response.name());
        assertEquals("borae-12345678", response.slug());
    }

    @Test
    void managerCanUpdateRestaurantProfile() {
        grantRole(RestaurantRole.MANAGER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.existsBySlugAndIdNot("borae-centro", restaurantId)).thenReturn(false);

        RestaurantProfileResponse response = restaurantService.updateProfile(
                restaurantId,
                new UpdateRestaurantProfileRequest("Boraê Centro", "Nova descrição", "borae-centro"),
                owner
        );

        assertEquals("Boraê Centro", response.name());
        assertEquals("borae-centro", response.slug());
        assertEquals("Nova descrição", response.description());
    }

    @Test
    void staffCannotUpdateRestaurantProfile() {
        grantRole(RestaurantRole.STAFF);

        assertThrows(ResponseStatusException.class, () -> restaurantService.updateProfile(
                restaurantId,
                new UpdateRestaurantProfileRequest("Nome", null, null),
                owner
        ));
    }

    @Test
    void memberCannotAccessAnotherRestaurantsProfile() {
        when(membershipRepository.findByUser_IdAndRestaurant_IdAndActiveTrue(owner.getId(), restaurantId))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> restaurantService.getProfile(restaurantId, owner));
    }

    @Test
    void duplicateSlugIsRejected() {
        grantRole(RestaurantRole.OWNER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(restaurantRepository.existsBySlugAndIdNot("existing-slug", restaurantId)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> restaurantService.updateProfile(
                restaurantId,
                new UpdateRestaurantProfileRequest(null, null, "existing-slug"),
                owner
        ));
    }

    private void grantRole(RestaurantRole role) {
        RestaurantMembership membership = new RestaurantMembership(owner, restaurant, role);
        when(membershipRepository.findByUser_IdAndRestaurant_IdAndActiveTrue(owner.getId(), restaurantId))
                .thenReturn(Optional.of(membership));
    }
}
