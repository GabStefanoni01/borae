package com.borae.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {
    boolean existsBySlugAndIdNot(String slug, UUID id);
}
