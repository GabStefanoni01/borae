ALTER TABLE restaurant
    ADD COLUMN phone VARCHAR(30),
    ADD COLUMN whatsapp VARCHAR(30),
    ADD COLUMN instagram VARCHAR(120),
    ADD COLUMN website VARCHAR(255),
    ADD COLUMN address VARCHAR(500),
    ADD COLUMN latitude NUMERIC(10, 7),
    ADD COLUMN longitude NUMERIC(10, 7),
    ADD COLUMN category VARCHAR(80),
    ADD COLUMN publication_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

ALTER TABLE restaurant
    ADD CONSTRAINT ck_restaurant_publication_status
        CHECK (publication_status IN ('DRAFT', 'PUBLISHED', 'SUSPENDED')),
    ADD CONSTRAINT ck_restaurant_coordinates_pair
        CHECK ((latitude IS NULL AND longitude IS NULL) OR (latitude IS NOT NULL AND longitude IS NOT NULL)),
    ADD CONSTRAINT ck_restaurant_latitude
        CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    ADD CONSTRAINT ck_restaurant_longitude
        CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180));
