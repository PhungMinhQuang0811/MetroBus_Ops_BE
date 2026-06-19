package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardAlertResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRecentIncidentResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRouteStationSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardTransactionTimelineResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardAlertItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardRecentIncidentItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardRouteStationSummaryItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardTransactionTimelineItemResponse;
import com.vdt.afc_ops_service.document.DeviceIncident;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.mapper.DashboardMapper;
import com.vdt.afc_ops_service.repository.BatchRepository;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.IDashboardService;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardBatchSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardControlSyncSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardDeviceSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardIncidentSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardTransactionSummaryResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardService implements IDashboardService {

    static final int MAX_DASHBOARD_RANGE_DAYS = 31;
    static final int MAX_RECENT_INCIDENT_LIMIT = 50;
    static final int MAX_ALERT_LIMIT = 20;

    TransactionRepository transactionRepository;
    DeviceRepository deviceRepository;
    BatchRepository batchRepository;
    StationControlSyncRepository stationControlSyncRepository;
    StationRepository stationRepository;
    MongoTemplate mongoTemplate;
    DashboardMapper dashboardMapper;
    SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(LocalDateTime from, LocalDateTime to, Long routeId, Long stationId) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        DashboardRange range = resolveRange(from, to);
        validateRouteStation(routeId, stationId);

        DashboardDeviceSummaryResponse deviceSummary = dashboardMapper.toDeviceSummary(
                deviceRepository.getDashboardDeviceSummary(operator.getId(), routeId, stationId));

        DashboardTransactionSummaryResponse transactionSummary = dashboardMapper.toTransactionSummary(
                transactionRepository.getDashboardTransactionSummary(operator.getId(), range.from(), range.to(), routeId, stationId));

        DashboardIncidentSummaryResponse incidentSummary = buildIncidentSummary(operator.getId(), range, routeId, stationId);

        DashboardBatchSummaryResponse batchSummary = dashboardMapper.toBatchSummary(
                batchRepository.getDashboardBatchSummary(operator.getId(), range.from(), range.to()));

        DashboardControlSyncSummaryResponse controlSyncSummary = dashboardMapper.toControlSyncSummary(
                stationControlSyncRepository.getDashboardControlSyncSummary(operator.getId(), range.from(), range.to(), routeId, stationId));

        return DashboardSummaryResponse.builder()
                .deviceSummary(deviceSummary)
                .transactionSummary(transactionSummary)
                .incidentSummary(incidentSummary)
                .batchSummary(batchSummary)
                .controlSyncSummary(controlSyncSummary)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardTransactionTimelineResponse getTransactionTimeline(LocalDateTime from, LocalDateTime to,
                                                                       Long routeId, Long stationId, String bucket) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        DashboardRange range = resolveRange(from, to);
        validateRouteStation(routeId, stationId);
        String normalizedBucket = normalizeBucket(bucket);

        List<Object[]> rows = transactionRepository.getDashboardTransactionTimeline(
                operator.getId(), range.from(), range.to(), routeId, stationId, normalizedBucket);

        List<DashboardTransactionTimelineItemResponse> items = dashboardMapper.toTransactionTimelineItems(rows);

        return DashboardTransactionTimelineResponse.builder()
                .bucket(normalizedBucket)
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardRouteStationSummaryResponse getRouteStationSummaries(LocalDateTime from, LocalDateTime to,
                                                                         Long routeId, Long stationId) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        DashboardRange range = resolveRange(from, to);
        validateRouteStation(routeId, stationId);

        List<Object[]> rows = transactionRepository.getDashboardRouteStationSummaries(
                operator.getId(), range.from(), range.to(), routeId, stationId);

        List<DashboardRouteStationSummaryItemResponse> items = dashboardMapper.toRouteStationSummaryItems(rows);

        return DashboardRouteStationSummaryResponse.builder()
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardRecentIncidentResponse getRecentIncidents(LocalDateTime from, LocalDateTime to,
                                                              Long routeId, Long stationId, String severity, int limit) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        DashboardRange range = resolveRange(from, to);
        validateRouteStation(routeId, stationId);
        validateLimit(limit, MAX_RECENT_INCIDENT_LIMIT);

        Criteria criteria = incidentBaseCriteria(operator.getId(), range.from(), range.to(), routeId, stationId);
        String normalizedSeverity = SearchFilterUtil.normalizeUppercase(severity);
        if (normalizedSeverity != null) {
            criteria = criteria.and("severity").is(normalizedSeverity);
        }

        Query query = Query.query(criteria).with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "occurredAt"))
                .limit(limit);
        List<DeviceIncident> incidents = mongoTemplate.find(query, DeviceIncident.class);
        Map<Long, Station> stationMap = loadStationMap(incidents.stream()
                .map(DeviceIncident::getStationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        List<DashboardRecentIncidentItemResponse> items = dashboardMapper.toRecentIncidentItems(incidents, stationMap);

        return DashboardRecentIncidentResponse.builder()
                .items(items)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardAlertResponse getAlerts(LocalDateTime from, LocalDateTime to,
                                            Long routeId, Long stationId, int limit) {
        securityUtils.getRequiredCurrentOperator();
        DashboardRange range = resolveRange(from, to);
        validateRouteStation(routeId, stationId);
        validateLimit(limit, MAX_ALERT_LIMIT);

        DashboardSummaryResponse summary = getSummary(range.from(), range.to(), routeId, stationId);
        DashboardRecentIncidentResponse recentIncidents = getRecentIncidents(range.from(), range.to(), routeId, stationId, null, Math.min(limit, 10));

        List<DashboardAlertItemResponse> items = new ArrayList<>();
        if (summary.getDeviceSummary().getOffline() > 0) {
            items.add(dashboardMapper.toAlert("DEVICE_OFFLINE", "HIGH",
                    summary.getDeviceSummary().getOffline() + " devices are offline",
                    "DEVICE", null));
        }
        if (summary.getIncidentSummary().getHigh() > 0) {
            items.add(dashboardMapper.toAlert("INCIDENT_HIGH", "HIGH",
                    summary.getIncidentSummary().getHigh() + " high-severity incidents detected",
                    "INCIDENT", null));
        }
        if (summary.getBatchSummary().getFailed() > 0) {
            items.add(dashboardMapper.toAlert("BATCH_FAILED", "MEDIUM",
                    summary.getBatchSummary().getFailed() + " batches failed",
                    "BATCH", null));
        }
        if (summary.getControlSyncSummary().getFailed() > 0) {
            items.add(dashboardMapper.toAlert("CONTROL_SYNC_FAILED", "MEDIUM",
                    summary.getControlSyncSummary().getFailed() + " control sync records failed",
                    "CONTROL_SYNC", null));
        }
        if (recentIncidents.getItems() != null && !recentIncidents.getItems().isEmpty()) {
            DashboardRecentIncidentItemResponse newest = recentIncidents.getItems().get(0);
            Map<Long, Station> newestStationMap;
            if (newest.getStationId() == null) {
                newestStationMap = Map.of();
            } else {
                newestStationMap = loadStationMap(Set.of(newest.getStationId()));
            }

            Station newestStation = newestStationMap.get(newest.getStationId());
            String newestStationCode;
            if (newestStation == null) {
                newestStationCode = null;
            } else {
                newestStationCode = newestStation.getStationCode();
            }

            String newestStationLabel;
            if (newestStationCode == null) {
                newestStationLabel = "unknown station";
            } else {
                newestStationLabel = newestStationCode;
            }

            items.add(dashboardMapper.toAlert("RECENT_INCIDENT", newest.getSeverity(),
                    "Latest incident at " + newestStationLabel,
                    "INCIDENT", newest.getIncidentId()));
        }

        return DashboardAlertResponse.builder()
                .items(items.stream().limit(limit).toList())
                .build();
    }

    private DashboardIncidentSummaryResponse buildIncidentSummary(Long operatorId, DashboardRange range,
                                                                  Long routeId, Long stationId) {
        long total = mongoTemplate.count(Query.query(incidentBaseCriteria(operatorId, range.from(), range.to(), routeId, stationId)), DeviceIncident.class);
        long open = mongoTemplate.count(Query.query(incidentBaseCriteria(operatorId, range.from(), range.to(), routeId, stationId).and("resolvedAt").is(null)), DeviceIncident.class);
        long high = mongoTemplate.count(Query.query(incidentBaseCriteria(operatorId, range.from(), range.to(), routeId, stationId).and("severity").is("HIGH")), DeviceIncident.class);
        return dashboardMapper.toIncidentSummary(total, open, high);
    }

    private Criteria incidentBaseCriteria(Long operatorId, LocalDateTime from, LocalDateTime to, Long routeId, Long stationId) {
        Criteria criteria = Criteria.where("operatorId").is(operatorId)
                .and("occurredAt").gte(from).lte(to);

        if (stationId != null && routeId != null) {
            List<Long> stationIdsInRoute = stationRepository.findIdsByRouteId(routeId);
            if (!stationIdsInRoute.contains(stationId)) {
                criteria = criteria.and("stationId").is(-1L);
            } else {
                criteria = criteria.and("stationId").is(stationId);
            }
        } else if (stationId != null) {
            criteria = criteria.and("stationId").is(stationId);
        } else if (routeId != null) {
            List<Long> stationIdsInRoute = stationRepository.findIdsByRouteId(routeId);
            if (stationIdsInRoute.isEmpty()) {
                criteria = criteria.and("stationId").is(-1L);
            } else {
                criteria = criteria.and("stationId").in(stationIdsInRoute);
            }
        }
        return criteria;
    }

    private DashboardRange resolveRange(LocalDateTime from, LocalDateTime to) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime queryFrom = from;
        LocalDateTime queryTo = to;

        if (queryFrom == null && queryTo == null) {
            queryFrom = now.minusHours(24);
            queryTo = now;
        } else if (queryFrom == null) {
            queryFrom = queryTo.minusHours(24);
        } else if (queryTo == null) {
            queryTo = now;
        }

        if (queryFrom.isAfter(queryTo)) {
            throw new AppException(ErrorCode.INVALID_DASHBOARD_TIME_RANGE);
        }
        if (queryFrom.plusDays(MAX_DASHBOARD_RANGE_DAYS).isBefore(queryTo)) {
            throw new AppException(ErrorCode.DASHBOARD_QUERY_TOO_WIDE);
        }
        return new DashboardRange(queryFrom, queryTo);
    }

    private String normalizeBucket(String bucket) {
        String normalized = SearchFilterUtil.normalizeUppercase(bucket);
        if (normalized == null) {
            return "hour";
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!"hour".equals(lower) && !"day".equals(lower)) {
            throw new AppException(ErrorCode.INVALID_DASHBOARD_BUCKET);
        }
        return lower;
    }

    private void validateRouteStation(Long routeId, Long stationId) {
        if (routeId != null && routeId <= 0) {
            throw new AppException(ErrorCode.INVALID_ROUTE_ID);
        }
        if (stationId != null && stationId <= 0) {
            throw new AppException(ErrorCode.INVALID_STATION_ID);
        }
    }

    private void validateLimit(int limit, int maxLimit) {
        if (limit < 1 || limit > maxLimit) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private Map<Long, Station> loadStationMap(Set<Long> stationIds) {
        if (stationIds == null || stationIds.isEmpty()) {
            return Map.of();
        }
        List<Station> stations = stationRepository.findAllById(stationIds);
        Map<Long, Station> stationMap = new HashMap<>();
        for (Station station : stations) {
            stationMap.put(station.getId(), station);
        }
        return stationMap;
    }

    private record DashboardRange(LocalDateTime from, LocalDateTime to) {}
}
