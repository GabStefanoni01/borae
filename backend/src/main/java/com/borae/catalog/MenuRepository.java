package com.borae.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuRepository extends JpaRepository<Menu, UUID> {
    List<Menu> findAllByRestaurant_IdOrderByDisplayOrderAscNameAsc(UUID restaurantId);
    List<Menu> findAllByRestaurant_IdAndStatusOrderByDisplayOrderAscNameAsc(UUID restaurantId, MenuStatus status);
    Optional<Menu> findByIdAndRestaurant_Id(UUID menuId, UUID restaurantId);
}
