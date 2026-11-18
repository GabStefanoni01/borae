CREATE TABLE assistant_daily_metrics (
    metric_date DATE NOT NULL,
    restaurant_id UUID NOT NULL,
    metric_key VARCHAR(40) NOT NULL,
    metric_value BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_assistant_daily_metrics_value CHECK (metric_value >= 0),
    CONSTRAINT uq_assistant_daily_metrics UNIQUE (metric_date, restaurant_id, metric_key)
);

CREATE INDEX idx_assistant_daily_metrics_date
    ON assistant_daily_metrics (metric_date);

CREATE TABLE assistant_daily_product_metrics (
    metric_date DATE NOT NULL,
    restaurant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    recommendation_count BIGINT NOT NULL DEFAULT 0,
    click_count BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_assistant_daily_product_recommendations CHECK (recommendation_count >= 0),
    CONSTRAINT ck_assistant_daily_product_clicks CHECK (click_count >= 0),
    CONSTRAINT uq_assistant_daily_product_metrics UNIQUE (metric_date, restaurant_id, product_id)
);

CREATE INDEX idx_assistant_daily_product_metrics_date
    ON assistant_daily_product_metrics (metric_date);
