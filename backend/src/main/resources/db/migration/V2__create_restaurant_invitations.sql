CREATE TABLE restaurant_invitation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_invitation_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant (id),
    CONSTRAINT fk_invitation_creator FOREIGN KEY (created_by) REFERENCES user_account (id),
    CONSTRAINT uk_restaurant_invitation_token_hash UNIQUE (token_hash),
    CONSTRAINT ck_restaurant_invitation_role CHECK (role IN ('MANAGER', 'STAFF'))
);

CREATE INDEX idx_restaurant_invitation_email ON restaurant_invitation (email);
CREATE INDEX idx_restaurant_invitation_restaurant ON restaurant_invitation (restaurant_id);
