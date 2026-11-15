CREATE TABLE restaurant_menu (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    restaurant_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_restaurant_menu_restaurant
        FOREIGN KEY (restaurant_id) REFERENCES restaurant (id),
    CONSTRAINT ck_restaurant_menu_status
        CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT ck_restaurant_menu_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX idx_restaurant_menu_restaurant_status_order
    ON restaurant_menu (restaurant_id, status, display_order);

CREATE TABLE menu_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    menu_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_menu_category_menu
        FOREIGN KEY (menu_id) REFERENCES restaurant_menu (id),
    CONSTRAINT ck_menu_category_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX idx_menu_category_menu_active_order
    ON menu_category (menu_id, active, display_order);
