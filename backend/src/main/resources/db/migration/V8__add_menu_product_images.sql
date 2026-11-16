ALTER TABLE restaurant_media
    DROP CONSTRAINT ck_restaurant_media_type;

ALTER TABLE restaurant_media
    ADD CONSTRAINT ck_restaurant_media_type
    CHECK (media_type IN ('LOGO', 'PHOTO', 'PRODUCT_IMAGE'));

CREATE TABLE menu_product_image (
    product_id UUID PRIMARY KEY REFERENCES menu_product(id) ON DELETE CASCADE,
    media_id UUID NOT NULL UNIQUE REFERENCES restaurant_media(id)
);
