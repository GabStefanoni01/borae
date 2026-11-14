package com.borae.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        assertEquals(RestaurantPublicationStatus.DRAFT, response.publicationStatus());
    }

    @Test
    void managerCanUpdateRestaurantProfileAndLocation() {
        grantRole(RestaurantRole.MANAGER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        RestaurantProfileResponse response = restaurantService.updateProfile(
                restaurantId,
                request("Boraê Centro", "Nova descrição", "borae-centro",
                        "11999990000", null, "@borae", "https://borae.example",
                        "Rua Central, 100", " -23.5505200 ", "-46.6333080", "Brasileira"),
                owner
        );

        assertEquals("Boraê Centro", response.name());
        assertEquals("borae-centro", response.slug());
        assertEquals("Nova descrição", response.description());
        assertEquals("11999990000", response.phone());
        assertEquals("@borae", response.instagram());
        assertEquals("Rua Central, 100", response.address());
        assertEquals(new BigDecimal("-23.5505200"), response.latitude());
        assertEquals(new BigDecimal("-46.6333080"), response.longitude());
        assertEquals("Brasileira", response.category());
    }

    @Test
    void staffCannotUpdateRestaurantProfile() {
        grantRole(RestaurantRole.STAFF);

        assertThrows(ResponseStatusException.class, () -> restaurantService.updateProfile(
                restaurantId, request("Nome", null, null, null, null, null, null, null, null, null, null), owner
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
                restaurantId, request(null, null, "existing-slug", null, null, null, null, null, null, null, null), owner
        ));
    }

    @Test
    void blankNameIsRejectedAfterTrimming() {
        grantRole(RestaurantRole.OWNER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        assertThrows(ResponseStatusException.class, () -> restaurantService.updateProfile(
                restaurantId, request("   ", null, null, null, null, null, null, null, null, null, null), owner
        ));
    }

    @Test
    void coordinatesMustBeProvidedAsAPair() {
        grantRole(RestaurantRole.OWNER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));

        assertThrows(ResponseStatusException.class, () -> restaurantService.updateProfile(
                restaurantId, request(null, null, null, null, null, null, null, null, new BigDecimal("-23.5"), null, null), owner
        ));
    }

    private UpdateRestaurantProfileRequest request(
            String name, String description, String slug, String phone, String whatsapp,
            String instagram, String website, String address, String latitude, String longitude, String category
    ) {
        return new UpdateRestaurantProfileRequest(
                name, description, slug, phone, whatsapp, instagram, website, address,
                latitude == null ? null : new BigDecimal(latitude.trim()),
                longitude == null ? null : new BigDecimal(longitude.trim()),
                category
        );
    }

    private void grantRole(RestaurantRole role) {
        RestaurantMembership membership = new RestaurantMembership(owner, restaurant, role);
        when(membershipRepository.findByUser_IdAndRestaurant_IdAndActiveTrue(owner.getId(), restaurantId))
                .thenReturn(Optional.of(membership));
    }
}
