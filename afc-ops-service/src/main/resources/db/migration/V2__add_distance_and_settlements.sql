-- ============================================================
-- Migration: Add station distance & operator settlements
-- ============================================================

-- 1. Add distance column to stations table
ALTER TABLE stations ADD COLUMN IF NOT EXISTS distance DECIMAL(5, 2) NOT NULL DEFAULT 0.00;

COMMENT ON COLUMN stations.distance IS 'Cumulative distance (km) from the first station of the route to this station';

-- 2. Create operator_settlements table
CREATE TABLE IF NOT EXISTS operator_settlements (
    id BIGSERIAL PRIMARY KEY,
    settlement_id VARCHAR(36) NOT NULL,
    period VARCHAR(30) NOT NULL,
    operator_code VARCHAR(50) NOT NULL,
    allocated_amount DECIMAL(15, 2) NOT NULL,
    total_km DECIMAL(10, 2) NOT NULL,
    total_trips INT NOT NULL,
    km_ratio DECIMAL(5, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE operator_settlements IS 'Revenue allocation settled from Level 5 clearing system';

CREATE INDEX IF NOT EXISTS idx_operator_settlements_settlement_id ON operator_settlements(settlement_id, operator_code);
CREATE INDEX IF NOT EXISTS idx_operator_settlements_operator_code ON operator_settlements(operator_code, created_at DESC);