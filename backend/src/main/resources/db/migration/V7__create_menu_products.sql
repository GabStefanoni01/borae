CREATE TABLE menu_product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL REFERENCES menu_category(id),
    name VARCHAR(160) NOT NULL,
    description VARCHAR(1000),
    price_in_cents BIGINT NOT NULL,
    lifecycle_status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    availability VARCHAR(16) NOT NULL DEFAULT 'AVAILABLE',
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_menu_product_price CHECK (price_in_cents >= 0),
    CONSTRAINT ck_menu_product_status CHECK (lifecycle_status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_menu_product_availability CHECK (availability IN ('AVAILABLE', 'UNAVAILABLE')),
    CONSTRAINT ck_menu_product_order CHECK (display_order >= 0)
);

CREATE INDEX idx_menu_product_category_status_order
    ON menu_product (category_id, lifecycle_status, display_order);

CREATE TABLE menu_product_ingredient (
    product_id UUID NOT NULL REFERENCES menu_product(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL,
    ingredient VARCHAR(120) NOT NULL,
    PRIMARY KEY (product_id, display_order),
    CONSTRAINT ck_menu_product_ingredient_order CHECK (display_order >= 0)
);

CREATE TABLE menu_product_addon (
    product_id UUID NOT NULL REFERENCES menu_product(id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL,
    name VARCHAR(120) NOT NULL,
    price_delta_in_cents BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (product_id, display_order),
    CONSTRAINT ck_menu_product_addon_order CHECK (display_order >= 0),
    CONSTRAINT ck_menu_product_addon_price CHECK (price_delta_in_cents >= 0)
);

CREATE INDEX idx_menu_product_addon_active
    ON menu_product_addon (product_id, active, display_order);
