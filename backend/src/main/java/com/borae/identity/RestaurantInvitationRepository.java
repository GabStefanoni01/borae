package com.borae.identity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantInvitationRepository extends JpaRepository<RestaurantInvitation, UUID> {

    Optional<RestaurantInvitation> findByTokenHash(String tokenHash);

    boolean existsByRestaurant_IdAndEmailIgnoreCaseAndAcceptedAtIsNullAndExpiresAtAfter(
            UUID restaurantId,
            String email,
            Instant now
    );

    long countByCreatedBy_IdAndCreatedAtAfter(UUID createdById, Instant since);

    long countByRestaurant_IdAndCreatedAtAfter(UUID restaurantId, Instant since);
}
