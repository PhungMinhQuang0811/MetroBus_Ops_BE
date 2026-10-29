# Plan triển khai UC18

Tài liệu này chỉ theo dõi **UC18 - Dashboard vận hành Cấp 4**.
UC21 được tách sang file riêng để dễ bám tiến độ.

## 1. Mục tiêu

- Cho `OPERATOR_MANAGER` xem dashboard vận hành.
- Dữ liệu mặc định là 24 giờ gần nhất nếu không truyền `from/to`.
- FE ghép dashboard theo nhiều widget độc lập, không dùng một API lớn.

## 2. API đã làm cho UC18

### Summary

```http
GET /dashboard/summary?from=&to=&routeId=&stationId=
```

Response: `DashboardSummaryResponse`

### Transaction timeline

```http
GET /dashboard/transaction-timeline?from=&to=&routeId=&stationId=&bucket=hour
```

Response: `DashboardTransactionTimelineResponse`

### Route/station summary

```http
GET /dashboard/route-station-summaries?from=&to=&routeId=&stationId=
```

Response: `DashboardRouteStationSummaryResponse`

### Recent incidents

```http
GET /dashboard/recent-incidents?from=&to=&routeId=&stationId=&severity=&limit=10
```

Response: `DashboardRecentIncidentResponse`

### Alerts

```http
GET /dashboard/alerts?from=&to=&routeId=&stationId=&limit=10
```

Response: `DashboardAlertResponse`

## 3. Response contracts

### `DashboardSummaryResponse`

- `deviceSummary.active`
- `deviceSummary.offline`
- `deviceSummary.maintenance`
- `deviceSummary.disabled`
- `transactionSummary.total`
- `transactionSummary.openGate`
- `transactionSummary.deny`
- `transactionSummary.acceptedForForwarding`
- `transactionSummary.denyRate`
- `incidentSummary.total`
- `incidentSummary.open`
- `incidentSummary.high`
- `batchSummary.total`
- `batchSummary.created`
- `batchSummary.submitted`
- `batchSummary.accepted`
- `batchSummary.rejected`
- `batchSummary.failed`
- `controlSyncSummary.total`
- `controlSyncSummary.pending`
- `controlSyncSummary.applied`
- `controlSyncSummary.failed`

### `DashboardTransactionTimelineResponse`

- `bucket`
- `items[].timePoint`
- `items[].total`
- `items[].openGate`
- `items[].deny`
- `items[].acceptedForForwarding`

### `DashboardRouteStationSummaryResponse`

- `items[].routeId`
- `items[].routeCode`
- `items[].routeName`
- `items[].stationId`
- `items[].stationCode`
- `items[].stationName`
- `items[].total`
- `items[].openGate`
- `items[].deny`

### `DashboardRecentIncidentResponse`

- `items[].incidentId`
- `items[].occurredAt`
- `items[].stationId`
- `items[].stationCode`
- `items[].deviceId`
- `items[].deviceCode`
- `items[].severity`
- `items[].incidentType`
- `items[].resolved`

### `DashboardAlertResponse`

- `items[].type`
- `items[].severity`
- `items[].message`
- `items[].resourceType`
- `items[].resourceId`

## 4. FE ghép màn

- Summary cards dùng `/dashboard/summary`
- Chart theo thời gian dùng `/dashboard/transaction-timeline`
- Bảng route/station dùng `/dashboard/route-station-summaries`
- Danh sách incident gần nhất dùng `/dashboard/recent-incidents`
- Panel cảnh báo dùng `/dashboard/alerts`

## 5. Error mapping

| Code | Error name | HTTP status | Message |
| --- | --- | --- | --- |
| `2001` | `INVALID_PAGE_REQUEST` | `400 Bad Request` | `Page must be >= 0 and size must be between 1 and 100` |
| `2004` | `INVALID_ROUTE_ID` | `400 Bad Request` | `Route id is invalid` |
| `2008` | `INVALID_STATION_ID` | `400 Bad Request` | `Station id is invalid` |
| `2030` | `INVALID_DASHBOARD_TIME_RANGE` | `400 Bad Request` | `Dashboard from time must be before or equal to to time` |
| `2031` | `INVALID_DASHBOARD_BUCKET` | `400 Bad Request` | `Invalid dashboard bucket` |
| `3033` | `DASHBOARD_QUERY_TOO_WIDE` | `400 Bad Request` | `Dashboard query range is too wide` |
| `4000` | `UNCATEGORIZED_EXCEPTION` | `500 Internal Server Error` | `Uncategorized error` |
| `4002` | `UNAUTHENTICATED` | `401 Unauthorized` | `Unauthenticated access` |
| `4006` | `ACCOUNT_DISABLED` | `403 Forbidden` | `Your account is currently disabled or inactive.` |
| `4007` | `ACCESS_DENIED` | `403 Forbidden` | `You do not have permission to access this resource` |
| `4009` | `INVALID_CSRF_TOKEN` | `403 Forbidden` | `Missing or invalid CSRF token` |
| `4012` | `OPERATOR_SCOPE_REQUIRED` | `403 Forbidden` | `Operator scope is required` |
| `4013` | `OPERATOR_ACCESS_DENIED` | `403 Forbidden` | `You do not have permission to access data from another operator` |

## 6. Empty state

- Không có dữ liệu thì `summary` trả `0`.
- Các endpoint list trả `items: []`.
- Route/station không khớp hiện tại trả empty result, không ném lỗi.

## 7. Unit test đã thêm

- `DashboardServiceTest`
- `DashboardControllerTest`

Target:
- method coverage 100%
- line coverage 90%
- branch coverage 80%

## 8. Postman test scenarios

1. Summary default 24h.
2. Summary có `from/to/routeId/stationId`.
3. Timeline với `bucket=hour`.
4. Timeline với `bucket=day`.
5. Route/station summary.
6. Recent incidents với `severity` và `limit`.
7. Alerts.
8. Empty state.
9. Invalid time range.
10. Invalid bucket.
11. Query quá rộng.
12. Invalid route/station/limit.
13. Unauthorized/forbidden.

## 9. Done when

- Có đủ 5 endpoint dashboard.
- FE ghép được dashboard theo widget.
- Error code và message rõ ràng.
- Test service/controller đã có.
