package com.borae.identity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantQrCodeServiceTest {

    @Mock private RestaurantRepository restaurantRepository;
    @Mock private RestaurantMembershipRepository membershipRepository;

    private RestaurantQrCodeService service() {
        return new RestaurantQrCodeService(
                restaurantRepository, membershipRepository, "https://borae.example/r/");
    }

    @Test
    void ownerCanGeneratePngPointingToPublicRestaurantUrl() {
        UserAccount owner = new UserAccount("Owner", "owner@example.com", "hash");
        Restaurant restaurant = new Restaurant("Boraê", "borae-12345678", null);
        when(membershipRepository.findByUser_IdAndRestaurant_IdAndActiveTrue(owner.getId(), restaurant.getId()))
                .thenReturn(Optional.of(new RestaurantMembership(owner, restaurant, RestaurantRole.OWNER)));
        when(restaurantRepository.findById(restaurant.getId())).thenReturn(Optional.of(restaurant));

        byte[] png = service().generateRestaurantQrCode(restaurant.getId(), owner);

        assertTrue(png.length > 100);
        assertArrayEquals(new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47}, new byte[]{png[0], png[1], png[2], png[3]});
    }

    @Test
    void staffCannotGenerateRestaurantQrCode() {
        UserAccount staff = new UserAccount("Staff", "staff@example.com", "hash");
        Restaurant restaurant = new Restaurant("Boraê", "borae-12345678", null);
        when(membershipRepository.findByUser_IdAndRestaurant_IdAndActiveTrue(staff.getId(), restaurant.getId()))
                .thenReturn(Optional.of(new RestaurantMembership(staff, restaurant, RestaurantRole.STAFF)));

        assertThrows(ResponseStatusException.class,
                () -> service().generateRestaurantQrCode(restaurant.getId(), staff));
        verify(restaurantRepository, never()).findById(restaurant.getId());
    }
}
