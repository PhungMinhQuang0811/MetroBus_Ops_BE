# Kế hoạch Refactor C4 — Card + Ticket theo mô hình C5

Tài liệu này mô tả kế hoạch refactor C4 để đồng bộ `Card` và `Ticket` với C5. Mục tiêu: gỡ bỏ `Entitlement`, hợp nhất vào `Ticket`, và cập nhật listener để nhận dữ liệu từ C5 đúng format.

Nguồn tham khảo:
- Mã nguồn C5 (`Afc-system`): entity `Card.java`, `Ticket.java` (domain model)
- `document/api-docs.yaml`: OpenAPI của C5
- `document/Draft Schema auth_db và afc_ops_db.md`: schema hiện tại của C4

---

## 1. Hiện trạng vs Mục tiêu

### C5 Model (source of truth)

```
Card (physical media)
├── id: UUID
├── cardUid: String (chip serial)
├── status: CREATED | ISSUED | ACTIVE | SUSPENDED | REVOKED
├── type: IDENTIFIED | ANON
├── supportsMetro: boolean
├── supportsBus: boolean
├── issuedAtStationId: UUID?
├── linkedUserId: UUID?
├── activatedAt / linkedAt / createdAt / updatedAt

Ticket (travel product — both single ride & monthly pass)
├── id: UUID
├── cardId: UUID?   (null = virtual / QR-based)
├── userId: UUID
├── type: SINGLE_TRIP | MONTHLY_PASS
├── status: ACTIVE | USED | EXPIRED
├── validFrom: LocalDate
├── validTo: LocalDate
├── price, fareRuleId, discountId
├── fromStationId, toStationId (nullable)
├── mode: METRO | BUS | ANY
├── scope: SINGLE_ROUTE | MULTI_ROUTE (nullable)
├── routeId (nullable)
├── purchasedAt, usedAt
```

### C4 (current — pre-refactor)

| Entity | Purpose | Status |
|--------|---------|--------|
| `Card` | Virtual QR container + physical card | Keep — add `supportsMetro/Bus` |
| `Ticket` | Single-ride ticket (`METRO_SINGLE_RIDE`) | **Refactor** — extend for both types |
| `Entitlement` | Monthly pass (`MONTHLY_PASS`) | **Delete** — merge into `Ticket` |
| `Transaction` | Has `entitlement_id` FK | **Update** — remove FK |

### C4 (target — post-refactor)

| Entity | C5 source | Notes |
|--------|-----------|-------|
| `Card` | C5 `Card` (physical) + C5 `Ticket(cardId=null)` (virtual placeholder) | Add `supportsMetro`, `supportsBus` |
| `Ticket` | C5 `Ticket(type=SINGLE_TRIP)` and `Ticket(type=MONTHLY_PASS)` | One table for all travel products |
| `Transaction` | C5 `Trip` | Only `ticket_id` FK (no `entitlement_id`) |

---

## 2. Step-by-Step Implementation

### Phase 1 — Update `Card` Entity

**File:** `afc-ops-service/src/main/java/.../entity/Card.java`

**Changes:**
- Add `supportsMetro: Boolean` (default `true`)
- Add `supportsBus: Boolean` (default `true`)

**Card.java concept after refactor:**
```
Before: Card = virtual QR container (cardType = VIRTUAL_QR/PHYSICAL)
After:  Card = storage for C5 Card data, handles both:
        - Physical card (C5 Card with cardUid)
        - Virtual card placeholder (when C5 sends Ticket with cardId=null, C4 auto-creates Card)
```

**Status mapping (C5 → C4):**
| C5 CardStatus | C4 Card.status |
|---------------|----------------|
| `CREATED`     | Ingest as `ACTIVE` or skip |
| `ISSUED`      | `ACTIVE` |
| `ACTIVE`      | `ACTIVE` |
| `SUSPENDED`   | `INACTIVE` |
| `REVOKED`     | `CANCELLED` |

### Phase 2 — Refactor `Ticket` Entity (merge Entitlement)

**File:** `afc-ops-service/src/main/java/.../entity/Ticket.java` (major rewrite)

**New fields to add:**
| Field | Type | Source | Notes |
|-------|------|--------|-------|
| `userId` | String(36) | C5 Ticket.userId | UUID of passenger |
| `type` | String(30) | C5 TicketType | `METRO_SINGLE_RIDE` or `MONTHLY_PASS` |
| `price` | BigDecimal(15,2) | C5 Ticket.price | Fare amount |
| `fareRuleId` | String(36) | C5 Ticket | Reference to fare rule |
| `discountId` | String(36) | C5 Ticket | Reference to discount |
| `fromStationRef` | String(100) | C5 Ticket.fromStationCode | Station code from C5 |
| `toStationRef` | String(100) | C5 Ticket.toStationCode | Station code from C5 |
| `mode` | String(30) | C5 FareMode | `METRO`, `BUS`, `ALL` |
| `scope` | String(30) | C5 PassScope | `SINGLE_ROUTE`, `MULTI_ROUTE` |
| `passengerType` | String(50) | From Entitlement | `STUDENT`, `PRIORITY` |
| `purchasedAt` | LocalDateTime | C5 Ticket.purchasedAt | When C5 created it |
| `usedAt` | LocalDateTime | C5 Ticket.usedAt | When marked used |

**Fields to keep from current Ticket:**
- `id`, `cardId`, `operatorRef`, `routeRef`, `validFrom`, `validTo`, `sourceVersion`, `syncedAt`, `updatedAt`
- `usageStatus` → rename to `status` (merge with Entitlement status)

**Fields to remove:**
- `routeScopeType` → replaced by `scope`
- `transportType` → replaced by `mode`
- `firstTapAt` → keep for TAP_IN tracking

**Status consolidation:**

| Current Ticket.usageStatus | Current Entitlement.status | New Ticket.status |
|---------------------------|---------------------------|-------------------|
| `UNUSED` | `ACTIVE` | `ACTIVE` |
| `IN_USE` | — | `IN_USE` |
| `USED` | — | `USED` |
| `EXPIRED` | `EXPIRED` | `EXPIRED` |
| `CANCELLED` | `CANCELLED` | `CANCELLED` |
| — | `INACTIVE` | `CANCELLED` |

### Phase 3 — Delete `Entitlement`

**Files to delete:**
- `entity/Entitlement.java`
- `repository/EntitlementRepository.java`

**Files to check for entitlement references (and update):**
- All service implementations
- All controller code
- All DTOs (request/response)

### Phase 4 — Update `Transaction` Entity

**File:** `entity/Transaction.java`

**Changes:**
- Remove `@ManyToOne Entitlement entitlement` FK
- Remove `entitlement_id` column
- Business rule becomes: `ticket_id` is the only product FK

### Phase 5 — DB Migration

**File:** `resources/db/migration/V3__merge_entitlement_into_ticket.sql`

```sql
-- 1. Add new C5 fields to tickets table
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS user_id VARCHAR(36);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS price DECIMAL(15,2);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS fare_rule_id VARCHAR(36);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS discount_id VARCHAR(36);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS from_station_ref VARCHAR(100);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS to_station_ref VARCHAR(100);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS mode VARCHAR(30);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS scope VARCHAR(30);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS passenger_type VARCHAR(50);
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS purchased_at TIMESTAMP;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS first_tap_at TIMESTAMP;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS used_at TIMESTAMP;

-- 2. Add C5 fields to cards table
ALTER TABLE cards ADD COLUMN IF NOT EXISTS supports_metro BOOLEAN DEFAULT TRUE;
ALTER TABLE cards ADD COLUMN IF NOT EXISTS supports_bus BOOLEAN DEFAULT TRUE;

-- 3. Migrate entitlements into tickets (merge data)
INSERT INTO tickets (id, card_id, type, mode, scope, operator_ref, route_ref,
                     from_station_ref, to_station_ref, usage_status,
                     valid_from, valid_to, passenger_type,
                     source_version, synced_at, updated_at)
SELECT e.id, e.card_id, 'MONTHLY_PASS', e.transport_type, e.pass_scope,
       e.operator_ref, e.route_ref, e.from_station_ref, e.to_station_ref,
       e.status, e.valid_from, e.valid_to, e.passenger_type,
       e.source_version, e.synced_at, e.updated_at
FROM entitlements e
WHERE NOT EXISTS (SELECT 1 FROM tickets t WHERE t.id = e.id);

-- 4. Drop entitlements table
DROP TABLE IF EXISTS entitlements;

-- 5. Remove entitlement_id from transactions
ALTER TABLE transactions DROP COLUMN IF EXISTS entitlement_id;
```

### Phase 6 — RabbitMQ Listener Refactor

**Files to create/update:**
- `integration/level5/dto/message/TicketMessage.java` — match C5's `TicketMessage` record:
  ```java
  public record TicketMessage(
      UUID ticketId, String type, String mode, String scope,
      UUID cardId, UUID userId,
      String fromStationCode, String toStationCode,
      BigDecimal fareAmount,
      LocalDate validFrom, LocalDate validTo,
      Instant issuedAt
  ) {}
  ```

**Files to update:**
- `integration/level5/listener/Level5TicketSyncListener.java` — handle both `SINGLE_TRIP` and `MONTHLY_PASS`
- `integration/level5/service/ILevel5TicketSyncService.java` — update interface
- `integration/level5/service/Impl/Level5TicketSyncService.java` — merge entitlement sync logic

**Files to delete:**
- `integration/level5/listener/Level5EntitlementSyncListener.java`
- `integration/level5/dto/message/entitlement/` directory (if exists)

**Config changes:**
- `config/MessageBrokerConfig.java` — remove entitlement queue/binding
- `resources/application.yaml` — remove entitlement routing keys, update ticket routing keys

**New listener logic:**
```java
@RabbitListener(queues = "#{level5TicketSyncProperties.queue()}")
public void receiveTicketSync(Message message) {
    TicketMessage msg = readPayload(message, TicketMessage.class);
    switch (msg.type()) {
        case "SINGLE_TRIP" -> upsertTicket(msg, "METRO_SINGLE_RIDE");
        case "MONTHLY_PASS" -> upsertTicket(msg, "MONTHLY_PASS");
    }
    // If cardId is null (virtual): auto-create placeholder Card
    if (msg.cardId() == null) {
        ensureCardExistsFromTicket(msg);
    }
}
```

### Phase 7 — Service Updates

**Files to update:**
| Service | Change |
|---------|--------|
| `service/Impl/DynamicQrService.java` | Replace Entitlement lookup with Ticket(type=MONTHLY_PASS) lookup |
| `service/Impl/TransactionService.java` | Remove Entitlement verification; use Ticket only |
| All services importing `Entitlement` | Replace with `Ticket` lookups |

### Phase 8 — Redis Cache Updates

**Key pattern changes:**
| Before | After |
|--------|-------|
| `card:active-product:{cardId}` → returns `ticketId` OR `entitlementId` | `card:active-product:{cardId}` → returns `ticketId` (type determines usage) |
| `ticket:{ticketId}` (single ride only) | `ticket:{ticketId}` (both types) |
| `entitlement:{entitlementId}` | **Remove** |

### Phase 9 — Remove Entitlement DTOs

- Delete `dto/response/entitlement/` directory
- Remove entitlement type imports from `dto/response/level5/`
- Update `dto/common.ts` on FE side to remove Entitlement types

### Phase 10 — Update Tests

**Files to update:**
- `DynamicQrServiceTest.java` — change entitlement assertions to ticket(type=MONTHLY_PASS)
- `TransactionServiceTest.java` — remove entitlement test scenarios
- `Level5TicketSyncListenerTest.java` (if exists) — add MONTHLY_PASS scenarios
- **Delete:** `EntitlementServiceTest.java` (if exists)

---

## 3. File Change Summary

| Type | File | Action |
|------|------|--------|
| 🟢 UPDATE | `entity/Card.java` | Add `supportsMetro`, `supportsBus` |
| 🟢 UPDATE | `entity/Ticket.java` | Add 12+ fields, merge Entitlement |
| 🔴 DELETE | `entity/Entitlement.java` | Remove |
| 🟢 UPDATE | `entity/Transaction.java` | Remove entitlement FK |
| 🔴 DELETE | `repository/EntitlementRepository.java` | Remove |
| 🟢 UPDATE | `repository/TicketRepository.java` | Update queries |
| 🔴 DELETE | `integration/level5/listener/Level5EntitlementSyncListener.java` | Remove |
| 🟢 UPDATE | `integration/level5/listener/Level5TicketSyncListener.java` | Handle both types |
| 🟢 NEW | `integration/level5/dto/message/TicketMessage.java` | Match C5 format |
| 🟢 UPDATE | `integration/level5/service/` | Merge entitlement logic |
| 🟢 UPDATE | `config/MessageBrokerConfig.java` | Remove entitlement queue |
| 🟢 UPDATE | `resources/application.yaml` | Remove entitlement routing keys |
| 🟢 NEW | `resources/db/migration/V3__merge_entitlement_into_ticket.sql` | Migration script |
| 🟢 UPDATE | `service/Impl/DynamicQrService.java` | Ticket-only product lookup |
| 🟢 UPDATE | `service/Impl/TransactionService.java` | Remove entitlement logic |
| 🟢 UPDATE | Various test files | Update assertions |

---

## 4. Impact Summary

| Metric | Value |
|--------|-------|
| Files to **create** | 2 (TicketMessage.java, V3 migration) |
| Files to **update** | ~10-12 |
| Files to **delete** | ~5 |
| DB tables dropped | 1 (`entitlements`) |
| DB tables modified | 2 (`tickets`, `cards`) |
| Redis keys removed | 1 (`entitlement:{id}`) |
| RabbitMQ queues removed | 1 (`level5-entitlement-sync`) |