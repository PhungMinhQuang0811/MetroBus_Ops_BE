# Kế hoạch triển khai Phân hệ Ca kíp (Shift Management)

## 1. Nghiệp vụ

### 1.1. Mô tả
Nhân viên ga (`STATION_OPERATOR`) cần check-in khi bắt đầu ca trực và check-out khi kết thúc ca.
Quản lý (`OPERATOR_MANAGER`) cần xem ai đang trực ở ga nào, lịch sử ca trực.

### 1.2. Luồng chính

```
Nhân viên A đến ga Bến Thành lúc 06:00
├── POST /shifts/check-in { stationId: 1 }
├── System: tạo shift, status = CHECKED_IN
└── Trả { shiftId, checkedInAt: 06:00, status: CHECKED_IN }

Nhân viên A kết thúc ca lúc 14:00
├── POST /shifts/check-out
├── System: đếm transaction trong ca (142), update status = CHECKED_OUT
└── Trả { shiftId, totalTransactions: 142, checkedOutAt: 14:00 }

Quản lý xem danh sách
├── GET /shifts/list?stationId=&status=&page=0&size=20
└── Trả danh sách shifts của operator
```

### 1.3. Actor & Quyền

| Actor | Quyền | Ghi chú |
|-------|-------|---------|
| `STATION_OPERATOR` | `SHIFT_WRITE` | Check-in, check-out. Chỉ thao tác trên ca của mình |
| `OPERATOR_MANAGER` | `SHIFT_READ` | Xem danh sách/all shifts trong operator |

---

## 2. Database (PostgreSQL - `afc_ops_db`)

### 2.1. Bảng `station_shifts`

```sql
CREATE TABLE station_shifts (
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

CREATE INDEX idx_shifts_account ON station_shifts(account_id);
CREATE INDEX idx_shifts_station ON station_shifts(station_id);
CREATE INDEX idx_shifts_status ON station_shifts(status);
CREATE INDEX idx_shifts_checked_in ON station_shifts(checked_in_at);
```

### 2.2. Migration

Tạo file: `afc-ops-service/src/main/resources/db/migration/V{next}__create_station_shifts_table.sql`

---

## 3. Backend - Danh sách File

### 3.1. Entity

**File:** `afc-ops-service/src/main/java/com/vdt/afc_ops_service/entity/StationShift.java`

```java
@Entity
@Table(name = "station_shifts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationShift {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, length = 36)
    private String accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "total_transactions", nullable = false)
    private Integer totalTransactions;

    @Column(name = "checked_in_at", nullable = false)
    private LocalDateTime checkedInAt;

    @Column(name = "checked_out_at")
    private LocalDateTime checkedOutAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
```

### 3.2. Repository

**File:** `afc-ops-service/src/main/java/com/vdt/afc_ops_service/repository/StationShiftRepository.java`

```java
public interface StationShiftRepository extends JpaRepository<StationShift, Long> {
    Optional<StationShift> findTopByAccountIdAndStatusOrderByCheckedInAtDesc(
        String accountId, String status);
    List<StationShift> findByStationIdOrderByCheckedInAtDesc(Long stationId, Pageable pageable);
    List<StationShift> findByStation_OperatorIdOrderByCheckedInAtDesc(
        Long operatorId, Pageable pageable);
    Optional<StationShift> findByAccountIdAndStatus(String accountId, String status);
    Page<StationShift> findByStatus(String status, Pageable pageable);
}
```

### 3.3. DTOs

#### Request

**File:** `dto/request/shift/CheckInRequest.java`
```java
@Data
public class CheckInRequest {
    @NotNull
    private Long stationId;
}
```

**File:** `dto/request/shift/ShiftListRequest.java` (dùng query params)
- `stationId` (optional) Long
- `status` (optional) String
- `page` int, default 0
- `size` int, default 20

#### Response

**File:** `dto/response/shift/ShiftResponse.java`
```java
@Data
@Builder
public class ShiftResponse {
    private Long id;
    private String accountId;
    private Long stationId;
    private String stationCode;
    private String stationName;
    private String status;
    private Integer totalTransactions;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
    private LocalDateTime createdAt;
}
```

**File:** `dto/response/shift/CheckInResponse.java`
```java
@Data
@Builder
public class CheckInResponse {
    private Long shiftId;
    private String accountId;
    private Long stationId;
    private String status;
    private LocalDateTime checkedInAt;
}
```

**File:** `dto/response/shift/CheckOutResponse.java`
```java
@Data
@Builder
public class CheckOutResponse {
    private Long shiftId;
    private String status;
    private Integer totalTransactions;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;
}
```

### 3.4. Mapper

**File:** `mapper/ShiftMapper.java`
```java
@Component
public class ShiftMapper {
    public ShiftResponse toResponse(StationShift shift) { ... }
    public CheckInResponse toCheckInResponse(StationShift shift) { ... }
    public CheckOutResponse toCheckOutResponse(StationShift shift, int totalTx) { ... }
}
```

### 3.5. Service Interface

**File:** `service/IShiftService.java`

```java
public interface IShiftService {
    CheckInResponse checkIn(String accountId, Long stationId);
    CheckOutResponse checkOut(String accountId);
    PageResponse<ShiftResponse> listShifts(Long stationId, String status,
        int page, int size);
}
```

### 3.6. Service Implementation

**File:** `service/impl/ShiftService.java`

```java
@Service
@RequiredArgsConstructor
public class ShiftService implements IShiftService {

    private final StationShiftRepository shiftRepository;
    private final StationRepository stationRepository;
    private final TransactionRepository transactionRepository;
    private final ShiftMapper shiftMapper;

    @Transactional
    public CheckInResponse checkIn(String accountId, Long stationId) {
        // 1. Validate station tồn tại và ACTIVE
        Station station = stationRepository.findById(stationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STATION_NOT_FOUND));

        // 2. Kiểm tra account chưa có shift CHECKED_IN
        Optional<StationShift> activeShift = shiftRepository
            .findByAccountIdAndStatus(accountId, "CHECKED_IN");
        if (activeShift.isPresent()) {
            throw new BusinessException(ErrorCode.SHIFT_ALREADY_CHECKED_IN);
        }

        // 3. Tạo shift mới
        StationShift shift = StationShift.builder()
            .accountId(accountId)
            .station(station)
            .status("CHECKED_IN")
            .totalTransactions(0)
            .checkedInAt(LocalDateTime.now())
            .build();

        shift = shiftRepository.save(shift);
        return shiftMapper.toCheckInResponse(shift);
    }

    @Transactional
    public CheckOutResponse checkOut(String accountId) {
        // 1. Tìm shift CHECKED_IN của account
        StationShift shift = shiftRepository
            .findByAccountIdAndStatus(accountId, "CHECKED_IN")
            .orElseThrow(() -> new BusinessException(ErrorCode.NO_ACTIVE_SHIFT));

        // 2. Đếm transaction trong ca
        int totalTx = transactionRepository
            .countByStationIdAndOccurredAtBetween(
                shift.getStation().getId(),
                shift.getCheckedInAt(),
                LocalDateTime.now());

        // 3. Update shift
        shift.setStatus("CHECKED_OUT");
        shift.setTotalTransactions(totalTx);
        shift.setCheckedOutAt(LocalDateTime.now());
        shift = shiftRepository.save(shift);

        return shiftMapper.toCheckOutResponse(shift, totalTx);
    }

    public PageResponse<ShiftResponse> listShifts(Long stationId, String status,
            int page, int size) {
        // Query with filters, operator scope from SecurityUtils
        // Return paginated ShiftResponse
    }
}
```

### 3.7. Controller

**File:** `controller/ShiftController.java`

```java
@RestController
@RequestMapping("/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final IShiftService shiftService;

    @PostMapping("/check-in")
    @PreAuthorize("hasAuthority('SHIFT_WRITE')")
    public ApiResponse<CheckInResponse> checkIn(@Valid @RequestBody CheckInRequest request) {
        String accountId = SecurityUtils.getCurrentAccountId();
        return ApiResponse.success(shiftService.checkIn(accountId, request.getStationId()));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAuthority('SHIFT_WRITE')")
    public ApiResponse<CheckOutResponse> checkOut() {
        String accountId = SecurityUtils.getCurrentAccountId();
        return ApiResponse.success(shiftService.checkOut(accountId));
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('SHIFT_READ')")
    public ApiResponse<PageResponse<ShiftResponse>> listShifts(
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(
            shiftService.listShifts(stationId, status, page, size));
    }
}
```

### 3.8. Permission & Security

**Sửa file:** `constant/PredefinedAfcPermission.java`
- Thêm 2 hằng:
  ```java
  public static final String SHIFT_READ = "SHIFT_READ";
  public static final String SHIFT_WRITE = "SHIFT_WRITE";
  ```
- Thêm 2 enum trong `Definition`:
  ```java
  SHIFT_READ("SHIFT_READ", "Xem lịch sử ca trực"),
  SHIFT_WRITE("SHIFT_WRITE", "Nhận ca và kết ca");
  ```

**Sửa file:** `constant/SecurityConstants.java`
- Thêm:
  ```java
  entry("/shifts/check-in", PredefinedAfcPermission.SHIFT_WRITE),
  entry("/shifts/check-out", PredefinedAfcPermission.SHIFT_WRITE),
  entry("/shifts/list", PredefinedAfcPermission.SHIFT_READ),
  ```

### 3.9. Error Codes

**Sửa file:** `common/exception/ErrorCode.java`

| Constant | Code | HTTP | Message |
|----------|------|------|---------|
| `STATION_NOT_FOUND` | 3005 | 404 | Station not found |
| `SHIFT_ALREADY_CHECKED_IN` | 2040 | 400 | You already have an active shift |
| `NO_ACTIVE_SHIFT` | 2041 | 400 | No active shift found |
| `SHIFT_NOT_FOUND` | 2042 | 404 | Shift not found |

### 3.10. Test

| File | Nội dung |
|------|----------|
| `ShiftServiceTest.java` | check-in thành công, check-in khi đã có active shift, check-out, check-out không có active shift, list shifts |
| `ShiftControllerTest.java` | 3 endpoints, auth, validation |

---

## 4. Frontend - Danh sách File

### 4.1. Route paths

**Sửa file:** `src/lib/routes.ts`
```typescript
export const ROUTES = {
  // ... existing
  manager: {
    // ... existing
    shifts: "/manager/shifts",
  },
  station: {
    // ... existing
    shifts: "/station/shifts",
  },
};
```

### 4.2. Navigation

**Sửa file:** `src/lib/navigation/portal-nav.ts`

```typescript
export const managerNavItems: PortalNavItem[] = [
  // ... existing
  { label: "Ca trực", href: ROUTES.manager.shifts },
];

export const stationNavItems: PortalNavItem[] = [
  // ... existing
  {
    label: "Ca trực",
    href: ROUTES.station.shifts,
    children: [
      { label: "Nhận ca / Kết ca", href: ROUTES.station.shifts },
    ],
  },
];
```

### 4.3. API endpoints

**Sửa file:** `src/lib/api/endpoints.ts`
```typescript
export const API_ENDPOINTS = {
  // ... existing
  shift: {
    checkIn: "/shifts/check-in",
    checkOut: "/shifts/check-out",
    listShifts: "/shifts/list",
  },
};
```

### 4.4. API Service

**File MỚI:** `src/lib/api/services/shift.ts`
```typescript
export const shiftApi = {
  checkIn: (stationId: number) => bffClient.post(API_ENDPOINTS.shift.checkIn, { stationId }),
  checkOut: () => bffClient.post(API_ENDPOINTS.shift.checkOut),
  listShifts: (params: ShiftsQuery) => bffClient.get(API_ENDPOINTS.shift.listShifts, { params }),
};
```

### 4.5. DTOs

**File MỚI:** `src/lib/api/dto/shift/check-in-request.ts`
```typescript
export interface CheckInRequest {
  stationId: number;
}
```

**File MỚI:** `src/lib/api/dto/shift/check-in-response.ts`
```typescript
export interface CheckInResponse {
  shiftId: number;
  accountId: string;
  stationId: number;
  status: string;
  checkedInAt: string;
}
```

**File MỚI:** `src/lib/api/dto/shift/check-out-response.ts`
```typescript
export interface CheckOutResponse {
  shiftId: number;
  status: string;
  totalTransactions: number;
  checkedInAt: string;
  checkedOutAt: string;
}
```

**File MỚI:** `src/lib/api/dto/shift/shift-response.ts`
```typescript
export interface ShiftResponse {
  id: number;
  accountId: string;
  stationId: number;
  stationCode: string;
  stationName: string;
  status: string;
  totalTransactions: number;
  checkedInAt: string;
  checkedOutAt: string | null;
  createdAt: string;
}
```

### 4.6. BFF Proxies

**File MỚI:** `src/app/bff/shift/check-in/route.ts`
```typescript
export async function POST(request: NextRequest) { ... }
```

**File MỚI:** `src/app/bff/shift/check-out/route.ts`
```typescript
export async function POST(request: NextRequest) { ... }
```

**File MỚI:** `src/app/bff/shift/list/route.ts`
```typescript
export async function GET(request: NextRequest) { ... }
```

### 4.7. Components

#### Station Operator Page

**File MỚI:** `src/components/shifts/station-shift-page.tsx`

Giao diện:
```
┌──────────────────────────────────────────────┐
│ Ca trực tại [Bến Thành]                      │
├──────────────────────────────────────────────┤
│                                              │
│  ┌──────────────────────────────┐            │
│  │  🟢 ĐANG TRỰC               │            │
│  │  Bắt đầu: 06:00 - 07/01/2026│            │
│  │  Giao dịch: 142             │            │
│  │                              │            │
│  │  [🔴 Kết ca]               │            │
│  └──────────────────────────────┘            │
│                                              │
│  hoặc (khi chưa check-in):                  │
│  ┌──────────────────────────────┐            │
│  │  ⚪ CHƯA NHẬN CA             │            │
│  │  Chọn ga: [Bến Thành ▼]     │            │
│  │                              │            │
│  │  [🟢 Nhận ca]              │            │
│  └──────────────────────────────┘            │
│                                              │
├──────────────────────────────────────────────┤
│ Lịch sử ca trực                              │
│ Ngày       | Bắt đầu| Kết thúc| GD         │
│ 07/01/2026 | 06:00  | 14:00  | 142        │
│ 06/30/2026 | 06:00  | 14:00  | 156        │
└──────────────────────────────────────────────┘
```

**Logic component:**
- Khi mount: gọi `GET /shifts/list?accountId=current&status=CHECKED_IN`
- Nếu có shift CHECKED_IN: hiển thị card đang trực + nút "Kết ca"
- Nếu không có: hiển thị card chọn ga + nút "Nhận ca"
- Sau check-in/check-out: refresh lịch sử

#### Manager Page

**File MỚI:** `src/components/shifts/manager-shift-page.tsx`

Giao diện:
```
┌──────────────────────────────────────────────┐
│ Ca trực toàn operator                        │
├──────────────────────────────────────────────┤
│ [Ga ▼] [Trạng thái: Tất cả ▼] [Lọc]        │
├──────────────────────────────────────────────┤
│ Ga          | NV   | Bắt đầu | KT  | GD    │
│ Bến Thành  | A    | 06:00   | 14:00| 142   │
│ Nhà hát    | B    | 06:30   | --  | 89    │ (đang trực)
└──────────────────────────────────────────────┘
```

### 4.8. Pages

**File MỚI:** `src/app/station/shifts/page.tsx`
```typescript
export default function StationShiftPage() {
  return <StationShiftPageComponent />;
}
```

**File MỚI:** `src/app/manager/shifts/page.tsx`
```typescript
export default function ManagerShiftPage() {
  // Sử dụng component chung, filter đơn giản hơn
}
```

---

## 5. Seed Data

**Sửa file:** `config/ApplicationInitConfig.java`
- Thêm `SHIFT_READ` vào role `OPERATOR_MANAGER`
- Thêm `SHIFT_WRITE` vào role `STATION_OPERATOR`
- Thêm `SHIFT_READ` vào role `STATION_OPERATOR` (cần xem lịch sử của mình)

---

## 6. Tổng quan thay đổi

### BE (8 files mới + 4 files sửa)

| # | Hành động | File |
|---|-----------|------|
| 1 | **MỚI** | `db/migration/V{next}__create_station_shifts_table.sql` |
| 2 | **MỚI** | `entity/StationShift.java` |
| 3 | **MỚI** | `repository/StationShiftRepository.java` |
| 4 | **MỚI** | `dto/request/shift/CheckInRequest.java` |
| 5 | **MỚI** | `dto/response/shift/CheckInResponse.java` |
| 6 | **MỚI** | `dto/response/shift/CheckOutResponse.java` |
| 7 | **MỚI** | `dto/response/shift/ShiftResponse.java` |
| 8 | **MỚI** | `mapper/ShiftMapper.java` |
| 9 | **MỚI** | `service/IShiftService.java` |
| 10 | **MỚI** | `service/impl/ShiftService.java` |
| 11 | **MỚI** | `controller/ShiftController.java` |
| 12 | SỬA | `constant/PredefinedAfcPermission.java` |
| 13 | SỬA | `constant/SecurityConstants.java` |
| 14 | SỬA | `common/exception/ErrorCode.java` |
| 15 | SỬA | `config/ApplicationInitConfig.java` |

### FE (10 files mới + 3 files sửa)

| # | Hành động | File |
|---|-----------|------|
| 1 | SỬA | `src/lib/routes.ts` |
| 2 | SỬA | `src/lib/navigation/portal-nav.ts` |
| 3 | SỬA | `src/lib/api/endpoints.ts` |
| 4 | **MỚI** | `src/lib/api/services/shift.ts` |
| 5 | **MỚI** | `src/lib/api/dto/shift/check-in-request.ts` |
| 6 | **MỚI** | `src/lib/api/dto/shift/check-in-response.ts` |
| 7 | **MỚI** | `src/lib/api/dto/shift/check-out-response.ts` |
| 8 | **MỚI** | `src/lib/api/dto/shift/shift-response.ts` |
| 9 | **MỚI** | `src/app/bff/shift/check-in/route.ts` |
| 10 | **MỚI** | `src/app/bff/shift/check-out/route.ts` |
| 11 | **MỚI** | `src/app/bff/shift/list/route.ts` |
| 12 | **MỚI** | `src/components/shifts/station-shift-page.tsx` |
| 13 | **MỚI** | `src/components/shifts/manager-shift-page.tsx` |
| 14 | **MỚI** | `src/app/station/shifts/page.tsx` |
| 15 | **MỚI** | `src/app/manager/shifts/page.tsx` |