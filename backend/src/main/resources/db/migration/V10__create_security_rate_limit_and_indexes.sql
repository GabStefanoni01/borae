CREATE TABLE security_rate_limit (
    bucket_key VARCHAR(200) PRIMARY KEY,
    request_count INTEGER NOT NULL DEFAULT 0,
    reset_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_security_rate_limit_key_length
        CHECK (char_length(bucket_key) BETWEEN 3 AND 200),
    CONSTRAINT ck_security_rate_limit_count
        CHECK (request_count >= 0)
);

CREATE INDEX idx_security_rate_limit_reset_at
    ON security_rate_limit (reset_at);

CREATE INDEX idx_restaurant_invitation_creator_created_at
    ON restaurant_invitation (created_by, created_at);

CREATE INDEX idx_restaurant_invitation_restaurant_created_at
    ON restaurant_invitation (restaurant_id, created_at);
