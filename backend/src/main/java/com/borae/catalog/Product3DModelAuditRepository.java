package com.borae.catalog;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface Product3DModelAuditRepository extends JpaRepository<Product3DModelAudit, UUID> {
}
