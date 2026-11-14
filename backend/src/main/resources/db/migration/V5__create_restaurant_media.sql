CREATE TABLE restaurant_media (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    media_type VARCHAR(16) NOT NULL,
    provider_file_id VARCHAR(200) NOT NULL UNIQUE,
    public_url VARCHAR(1000) NOT NULL,
    content_type VARCHAR(50) NOT NULL,
    file_size BIGINT NOT NULL,
    width INTEGER NOT NULL,
    height INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_restaurant_media_type CHECK (media_type IN ('LOGO', 'PHOTO')),
    CONSTRAINT ck_restaurant_media_size CHECK (file_size > 0),
    CONSTRAINT ck_restaurant_media_dimensions CHECK (width > 0 AND height > 0)
);

CREATE INDEX idx_restaurant_media_restaurant_type
    ON restaurant_media (restaurant_id, media_type, created_at);

CREATE UNIQUE INDEX uq_restaurant_media_single_logo
    ON restaurant_media (restaurant_id)
    WHERE media_type = 'LOGO';
