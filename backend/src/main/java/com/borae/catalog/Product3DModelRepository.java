package com.borae.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface Product3DModelRepository extends JpaRepository<Product3DModel, UUID> {
    Optional<Product3DModel> findByProduct_Id(UUID productId);
    List<Product3DModel> findAllByProduct_IdInAndStatus(Collection<UUID> productIds, Product3DModelStatus status);
}
