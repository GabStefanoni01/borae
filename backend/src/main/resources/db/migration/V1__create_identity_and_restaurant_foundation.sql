CREATE TABLE user_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    full_name VARCHAR(160) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_account_email UNIQUE (email)
);

CREATE TABLE restaurant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(180) NOT NULL,
    slug VARCHAR(120) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_restaurant_slug UNIQUE (slug)
);

CREATE TABLE restaurant_membership (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    restaurant_id UUID NOT NULL,
    role VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_membership_user
        FOREIGN KEY (user_id) REFERENCES user_account (id),
    CONSTRAINT fk_membership_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant (id),
    CONSTRAINT uk_membership_user_restaurant UNIQUE (user_id, restaurant_id),
    CONSTRAINT ck_membership_role
        CHECK (role IN ('OWNER', 'MANAGER', 'STAFF'))
);

CREATE INDEX idx_membership_restaurant ON restaurant_membership (restaurant_id);
CREATE INDEX idx_membership_user ON restaurant_membership (user_id);
