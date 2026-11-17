CREATE TABLE menu_product_3d_model (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL UNIQUE REFERENCES menu_product(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL,
    provider_file_id VARCHAR(200) NOT NULL UNIQUE,
    public_url VARCHAR(1000) NOT NULL,
    content_type VARCHAR(80) NOT NULL,
    file_size BIGINT NOT NULL,
    width_mm INTEGER NOT NULL,
    height_mm INTEGER NOT NULL,
    depth_mm INTEGER NOT NULL,
    created_by_user_id UUID NOT NULL REFERENCES user_account(id),
    updated_by_user_id UUID NOT NULL REFERENCES user_account(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_product_3d_model_status CHECK (status IN ('READY', 'PUBLISHED')),
    CONSTRAINT ck_product_3d_model_file_size CHECK (file_size > 0),
    CONSTRAINT ck_product_3d_model_dimensions CHECK (width_mm > 0 AND height_mm > 0 AND depth_mm > 0)
);

CREATE INDEX idx_product_3d_model_status ON menu_product_3d_model(status);

CREATE TABLE menu_product_3d_model_audit (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    actor_user_id UUID NOT NULL REFERENCES user_account(id),
    action VARCHAR(16) NOT NULL,
    provider_file_id VARCHAR(200),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_product_3d_model_audit_action
        CHECK (action IN ('UPLOADED', 'REPLACED', 'PUBLISHED', 'UNPUBLISHED', 'DELETED'))
);

CREATE INDEX idx_product_3d_model_audit_product_time
    ON menu_product_3d_model_audit(product_id, occurred_at DESC);
