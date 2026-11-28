package com.borae.catalog;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface Product3DGenerationJobRepository extends JpaRepository<Product3DGenerationJob, UUID> {
    Optional<Product3DGenerationJob> findByIdAndProduct_Id(UUID id, UUID productId);
    boolean existsByProduct_IdAndStatusIn(UUID productId, java.util.Collection<Product3DGenerationJobStatus> statuses);
}
