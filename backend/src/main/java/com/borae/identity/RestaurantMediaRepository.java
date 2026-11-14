package com.borae.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantMediaRepository extends JpaRepository<RestaurantMedia, UUID> {
    List<RestaurantMedia> findAllByRestaurant_IdOrderByCreatedAtDesc(UUID restaurantId);
    long countByRestaurant_IdAndMediaType(UUID restaurantId, RestaurantMediaType mediaType);
    Optional<RestaurantMedia> findByIdAndRestaurant_Id(UUID id, UUID restaurantId);
    Optional<RestaurantMedia> findFirstByRestaurant_IdAndMediaType(UUID restaurantId, RestaurantMediaType mediaType);
}
