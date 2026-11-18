CREATE TABLE assistant_ip_rate_limit (
    window_start TIMESTAMPTZ NOT NULL,
    ip_hash CHAR(64) NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_assistant_ip_rate_limit_count CHECK (request_count >= 0),
    CONSTRAINT uq_assistant_ip_rate_limit_window UNIQUE (window_start, ip_hash)
);

CREATE INDEX idx_assistant_ip_rate_limit_window
    ON assistant_ip_rate_limit (window_start);
