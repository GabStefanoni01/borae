CREATE TABLE assistant_daily_quota (
    id BIGSERIAL PRIMARY KEY,
    quota_date DATE NOT NULL,
    scope_type VARCHAR(20) NOT NULL,
    scope_key VARCHAR(64) NOT NULL,
    consumed INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_assistant_daily_quota_scope
        CHECK (scope_type IN ('CONSUMER_SESSION', 'RESTAURANT')),
    CONSTRAINT ck_assistant_daily_quota_consumed
        CHECK (consumed >= 0),
    CONSTRAINT uq_assistant_daily_quota_scope
        UNIQUE (quota_date, scope_type, scope_key)
);

CREATE INDEX idx_assistant_daily_quota_date
    ON assistant_daily_quota (quota_date);
