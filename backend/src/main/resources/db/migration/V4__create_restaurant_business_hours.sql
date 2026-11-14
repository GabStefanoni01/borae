CREATE TABLE restaurant_business_hours (
    id UUID PRIMARY KEY,
    restaurant_id UUID NOT NULL REFERENCES restaurant(id) ON DELETE CASCADE,
    day_of_week VARCHAR(16) NOT NULL,
    position INTEGER NOT NULL,
    opening_time TIME NOT NULL,
    closing_time TIME NOT NULL,
    CONSTRAINT ck_restaurant_business_hours_day
        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT ck_restaurant_business_hours_position
        CHECK (position >= 0),
    CONSTRAINT ck_restaurant_business_hours_interval
        CHECK (closing_time > opening_time),
    CONSTRAINT uq_restaurant_business_hours_position
        UNIQUE (restaurant_id, day_of_week, position)
);

CREATE INDEX idx_restaurant_business_hours_restaurant_day
    ON restaurant_business_hours (restaurant_id, day_of_week, position);
