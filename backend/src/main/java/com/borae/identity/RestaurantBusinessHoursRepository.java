package com.borae.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RestaurantBusinessHoursRepository extends JpaRepository<RestaurantBusinessHours, UUID> {

    List<RestaurantBusinessHours> findAllByRestaurant_IdOrderByDayOfWeekAscPositionAsc(UUID restaurantId);

    void deleteAllByRestaurant_Id(UUID restaurantId);
}
