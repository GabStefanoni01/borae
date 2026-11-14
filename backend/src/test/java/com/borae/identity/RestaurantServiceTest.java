package com.borae.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private RestaurantMembershipRepository membershipRepository;

    @Mock
    private RestaurantBusinessHoursRepository businessHoursRepository;

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
                        "Rua Central, 100", "-23.5505200", "-46.6333080", "Brasileira"),
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
                restaurantId, request(null, null, null, null, null, null, null, null, "-23.5", null, null), owner
        ));
    }

    @Test
    void managerCanReplaceWeeklyScheduleWithMultipleIntervals() {
        grantRole(RestaurantRole.MANAGER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        ReplaceBusinessHoursRequest request = new ReplaceBusinessHoursRequest(List.of(
                new BusinessHoursDayRequest(DayOfWeek.MONDAY, List.of(
                        new BusinessHoursIntervalRequest(LocalTime.of(14, 0), LocalTime.of(18, 0)),
                        new BusinessHoursIntervalRequest(LocalTime.of(9, 0), LocalTime.of(12, 0))
                )),
                new BusinessHoursDayRequest(DayOfWeek.TUESDAY, List.of(
                        new BusinessHoursIntervalRequest(LocalTime.of(10, 0), LocalTime.of(16, 0))
                ))
        ));

        List<BusinessHoursDayResponse> response =
                restaurantService.replaceBusinessHours(restaurantId, request, owner);

        assertEquals(2, response.size());
        assertEquals(DayOfWeek.MONDAY, response.get(0).dayOfWeek());
        assertEquals(LocalTime.of(9, 0), response.get(0).intervals().get(0).opensAt());
        assertEquals(LocalTime.of(14, 0), response.get(0).intervals().get(1).opensAt());
        verify(businessHoursRepository).deleteAllByRestaurant_Id(restaurantId);
        verify(businessHoursRepository).saveAll(anyList());
    }

    @Test
    void overlappingBusinessHoursAreRejectedWithoutReplacingExistingSchedule() {
        grantRole(RestaurantRole.OWNER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        ReplaceBusinessHoursRequest request = new ReplaceBusinessHoursRequest(List.of(
                new BusinessHoursDayRequest(DayOfWeek.MONDAY, List.of(
                        new BusinessHoursIntervalRequest(LocalTime.of(9, 0), LocalTime.of(13, 0)),
                        new BusinessHoursIntervalRequest(LocalTime.of(12, 0), LocalTime.of(17, 0))
                ))
        ));

        assertThrows(ResponseStatusException.class,
                () -> restaurantService.replaceBusinessHours(restaurantId, request, owner));
        verify(businessHoursRepository, never()).deleteAllByRestaurant_Id(restaurantId);
    }

    @Test
    void duplicateDayInWeeklyScheduleIsRejected() {
        grantRole(RestaurantRole.OWNER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        ReplaceBusinessHoursRequest request = new ReplaceBusinessHoursRequest(List.of(
                new BusinessHoursDayRequest(DayOfWeek.SUNDAY, List.of(
                        new BusinessHoursIntervalRequest(LocalTime.of(9, 0), LocalTime.of(12, 0))
                )),
                new BusinessHoursDayRequest(DayOfWeek.SUNDAY, List.of(
                        new BusinessHoursIntervalRequest(LocalTime.of(14, 0), LocalTime.of(18, 0))
                ))
        ));

        assertThrows(ResponseStatusException.class,
                () -> restaurantService.replaceBusinessHours(restaurantId, request, owner));
        verify(businessHoursRepository, never()).deleteAllByRestaurant_Id(restaurantId);
    }

    @Test
    void closingTimeMustBeAfterOpeningTime() {
        grantRole(RestaurantRole.OWNER);
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        ReplaceBusinessHoursRequest request = new ReplaceBusinessHoursRequest(List.of(
                new BusinessHoursDayRequest(DayOfWeek.FRIDAY, List.of(
                        new BusinessHoursIntervalRequest(LocalTime.of(18, 0), LocalTime.of(9, 0))
                ))
        ));

        assertThrows(ResponseStatusException.class,
                () -> restaurantService.replaceBusinessHours(restaurantId, request, owner));
        verify(businessHoursRepository, never()).deleteAllByRestaurant_Id(restaurantId);
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
