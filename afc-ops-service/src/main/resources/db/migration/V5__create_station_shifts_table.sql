CREATE TABLE IF NOT EXISTS station_shifts (
    id BIGSERIAL PRIMARY KEY,
    account_id VARCHAR(36) NOT NULL,
    station_id BIGINT NOT NULL REFERENCES stations(id),
    status VARCHAR(30) NOT NULL DEFAULT 'CHECKED_IN',
    total_transactions INT NOT NULL DEFAULT 0,
    checked_in_at TIMESTAMP NOT NULL,
    checked_out_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shifts_account ON station_shifts(account_id);
CREATE INDEX IF NOT EXISTS idx_shifts_station ON station_shifts(station_id);
CREATE INDEX IF NOT EXISTS idx_shifts_status ON station_shifts(status);
CREATE INDEX IF NOT EXISTS idx_shifts_checked_in ON station_shifts(checked_in_at);