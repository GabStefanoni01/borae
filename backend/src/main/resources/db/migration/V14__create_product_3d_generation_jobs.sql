CREATE TABLE product_3d_generation_job (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL REFERENCES menu_product(id) ON DELETE CASCADE,
    source_media_id UUID NOT NULL REFERENCES restaurant_media(id),
    requested_by_user_id UUID NOT NULL REFERENCES user_account(id),
    status VARCHAR(16) NOT NULL,
    engine VARCHAR(40),
    error_code VARCHAR(60),
    error_message VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_product_3d_generation_job_status
        CHECK (status IN ('QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX idx_product_3d_generation_job_product_created
    ON product_3d_generation_job(product_id, created_at DESC);

CREATE INDEX idx_product_3d_generation_job_queue
    ON product_3d_generation_job(status, created_at)
    WHERE status IN ('QUEUED', 'PROCESSING');

CREATE UNIQUE INDEX uq_product_3d_generation_job_active_product
    ON product_3d_generation_job(product_id)
    WHERE status IN ('QUEUED', 'PROCESSING');
