package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.document.DeviceIncident;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardAlertResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRecentIncidentResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRouteStationSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardTransactionTimelineResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardBatchSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardControlSyncSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardDeviceSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardIncidentSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardTransactionSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardAlertItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardRecentIncidentItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardRouteStationSummaryItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardTransactionTimelineItemResponse;
import com.vdt.afc_ops_service.entity.Station;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Component
public class DashboardMapper {

    public DashboardDeviceSummaryResponse toDeviceSummary(List<Object[]> rows) {
        Object[] row = firstOrEmpty(rows, 4);
        return DashboardDeviceSummaryResponse.builder()
                .active(toLong(row[0]))
                .offline(toLong(row[1]))
                .maintenance(toLong(row[2]))
                .disabled(toLong(row[3]))
                .build();
    }

    public DashboardTransactionSummaryResponse toTransactionSummary(List<Object[]> rows) {
        Object[] row = firstOrEmpty(rows, 4);
        long total = toLong(row[0]);
        long deny = toLong(row[2]);
        double denyRate = total == 0 ? 0.0 : BigDecimal.valueOf(deny)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP)
                .doubleValue();

        return DashboardTransactionSummaryResponse.builder()
                .total(total)
                .openGate(toLong(row[1]))
                .deny(deny)
                .acceptedForForwarding(toLong(row[3]))
                .denyRate(denyRate)
                .build();
    }

    public DashboardIncidentSummaryResponse toIncidentSummary(long total, long open, long high) {
        return DashboardIncidentSummaryResponse.builder()
                .total(total)
                .open(open)
                .high(high)
                .build();
    }

    public DashboardBatchSummaryResponse toBatchSummary(List<Object[]> rows) {
        Object[] row = firstOrEmpty(rows, 6);
        return DashboardBatchSummaryResponse.builder()
                .total(toLong(row[0]))
                .created(toLong(row[1]))
                .submitted(toLong(row[2]))
                .accepted(toLong(row[3]))
                .rejected(toLong(row[4]))
                .failed(toLong(row[5]))
                .build();
    }

    public DashboardControlSyncSummaryResponse toControlSyncSummary(List<Object[]> rows) {
        Object[] row = firstOrEmpty(rows, 4);
        return DashboardControlSyncSummaryResponse.builder()
                .total(toLong(row[0]))
                .pending(toLong(row[1]))
                .applied(toLong(row[2]))
                .failed(toLong(row[3]))
                .build();
    }

    public List<DashboardTransactionTimelineItemResponse> toTransactionTimelineItems(List<Object[]> rows) {
        return rows.stream()
                .map(row -> DashboardTransactionTimelineItemResponse.builder()
                        .timePoint(toLocalDateTime(row[0]))
                        .total(toLong(row[1]))
                        .openGate(toLong(row[2]))
                        .deny(toLong(row[3]))
                        .acceptedForForwarding(toLong(row[4]))
                        .build())
                .toList();
    }

    public List<DashboardRouteStationSummaryItemResponse> toRouteStationSummaryItems(List<Object[]> rows) {
        return rows.stream()
                .map(row -> DashboardRouteStationSummaryItemResponse.builder()
                        .routeId(toLongObject(row[0]))
                        .routeCode(toStringValue(row[1]))
                        .routeName(toStringValue(row[2]))
                        .stationId(toLongObject(row[3]))
                        .stationCode(toStringValue(row[4]))
                        .stationName(toStringValue(row[5]))
                        .total(toLong(row[6]))
                        .openGate(toLong(row[7]))
                        .deny(toLong(row[8]))
                        .build())
                .toList();
    }

    public List<DashboardRecentIncidentItemResponse> toRecentIncidentItems(List<DeviceIncident> incidents,
                                                                           Map<Long, Station> stationMap) {
        return incidents.stream()
                .map(incident -> DashboardRecentIncidentItemResponse.builder()
                        .incidentId(incident.getId())
                        .occurredAt(incident.getOccurredAt())
                        .stationId(incident.getStationId())
                        .stationCode(resolveStationCode(stationMap, incident.getStationId()))
                        .deviceId(incident.getDeviceId())
                        .deviceCode(incident.getDeviceCode())
                        .severity(incident.getSeverity())
                        .incidentType(incident.getIncidentType())
                        .resolved(incident.getResolvedAt() != null)
                        .build())
                .toList();
    }

    public DashboardAlertItemResponse toAlert(String type, String severity, String message,
                                              String resourceType, String resourceId) {
        return DashboardAlertItemResponse.builder()
                .type(type)
                .severity(severity)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .build();
    }

    private Object[] firstOrEmpty(List<Object[]> rows, int expectedSize) {
        if (rows == null || rows.isEmpty()) {
            return new Object[expectedSize];
        }
        return rows.get(0);
    }

    private long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private Long toLongObject(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private String toStringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof LocalDateTime localDateTime) {
            return localDateTime;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), ZoneId.systemDefault());
    }

    private String resolveStationCode(Map<Long, Station> stationMap, Long stationId) {
        if (stationMap == null || stationId == null) {
            return null;
        }
        Station station = stationMap.get(stationId);
        return station == null ? null : station.getStationCode();
    }
}
