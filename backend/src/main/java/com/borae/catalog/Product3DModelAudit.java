package com.borae.catalog;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "menu_product_3d_model_audit")
public class Product3DModelAudit {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(nullable = false, length = 16)
    private String action;

    @Column(name = "provider_file_id", length = 200)
    private String providerFileId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected Product3DModelAudit() {}

    public Product3DModelAudit(UUID productId, UUID actorUserId, String action, String providerFileId) {
        this.id = UUID.randomUUID();
        this.productId = productId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.providerFileId = providerFileId;
        this.occurredAt = Instant.now();
    }
}
