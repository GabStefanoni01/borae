package com.borae.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantMembershipRepository extends JpaRepository<RestaurantMembership, UUID> {

    @Query("""
            select membership
            from RestaurantMembership membership
            join fetch membership.restaurant restaurant
            where membership.user.id = :userId
              and membership.active = true
              and restaurant.active = true
            order by restaurant.name
            """)
    List<RestaurantMembership> findActiveMembershipsByUserId(@Param("userId") UUID userId);

    @Query("""
            select membership
            from RestaurantMembership membership
            join fetch membership.user user
            where membership.restaurant.id = :restaurantId
              and membership.active = true
            order by user.fullName
            """)
    List<RestaurantMembership> findActiveMembersByRestaurantId(@Param("restaurantId") UUID restaurantId);

    Optional<RestaurantMembership> findByUser_IdAndRestaurant_IdAndActiveTrue(UUID userId, UUID restaurantId);

    Optional<RestaurantMembership> findByUser_IdAndRestaurant_Id(UUID userId, UUID restaurantId);

    Optional<RestaurantMembership> findByIdAndRestaurant_IdAndActiveTrue(UUID membershipId, UUID restaurantId);

    boolean existsByUser_IdAndRestaurant_IdAndActiveTrue(UUID userId, UUID restaurantId);
}
