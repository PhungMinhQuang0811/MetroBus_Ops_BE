-- 1. Add new C5 fields to tickets table
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS user_id VARCHAR(36);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS price DECIMAL(15,2);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS fare_rule_id VARCHAR(36);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS discount_id VARCHAR(36);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS scope VARCHAR(30);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS mode VARCHAR(30);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS passenger_type VARCHAR(50);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS purchased_at TIMESTAMP;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS used_at TIMESTAMP;

-- 2. Add C5 fields to cards table
ALTER TABLE cards ADD COLUMN IF NOT EXISTS supports_metro BOOLEAN DEFAULT TRUE;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS supports_bus BOOLEAN DEFAULT TRUE;

-- 3. Make old columns nullable (new code uses scope/mode instead)
ALTER TABLE tickets ALTER COLUMN route_scope_type DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN transport_type DROP NOT NULL;
ALTER TABLE tickets ALTER COLUMN card_id DROP NOT NULL;

-- 4. Migrate entitlements into tickets (merge data)
INSERT INTO tickets (id, card_id, user_id, ticket_type, scope, mode, operator_ref, route_ref,
                     from_station_ref, to_station_ref, usage_status, route_scope_type, transport_type,
                     valid_from, valid_to, passenger_type,
                     source_version, synced_at, updated_at)
SELECT e.id, e.card_id, NULL, 'MONTHLY_PASS', e.pass_scope, e.transport_type,
       e.operator_ref, e.route_ref, e.from_station_ref, e.to_station_ref,
       e.status, e.pass_scope, e.transport_type,
       e.valid_from, e.valid_to, e.passenger_type,
       e.source_version, e.synced_at, e.updated_at
FROM entitlements e
WHERE NOT EXISTS (SELECT 1 FROM tickets t WHERE t.id = e.id);

-- 5. Drop FK referencing entitlements BEFORE dropping the table
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS fk_afc_transactions_entitlement;

-- 6. Drop entitlement_id from transactions
ALTER TABLE transactions DROP COLUMN IF EXISTS entitlement_id;

-- 7. Drop entitlements table (no more FK references)
DROP TABLE IF EXISTS entitlements;