package com.borae.identity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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
}
