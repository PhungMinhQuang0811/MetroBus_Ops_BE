package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.document.DeviceIncident;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardAlertResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRecentIncidentResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRouteStationSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardTransactionTimelineResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardRecentIncidentItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardRouteStationSummaryItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardTransactionTimelineItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardBatchSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardControlSyncSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardDeviceSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardIncidentSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardTransactionSummaryResponse;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.mapper.DashboardMapper;
import com.vdt.afc_ops_service.repository.BatchRepository;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.Impl.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DashboardServiceTest {

    @Mock
    private DeviceRepository deviceRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private BatchRepository batchRepository;
    @Mock
    private StationControlSyncRepository stationControlSyncRepository;
    @Mock
    private StationRepository stationRepository;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private Operator operator;

    @Spy
    private DashboardMapper dashboardMapper = new DashboardMapper();

    @InjectMocks
    private DashboardService dashboardService;

    private final LocalDateTime from = LocalDateTime.of(2026, 6, 18, 0, 0);
    private final LocalDateTime to = LocalDateTime.of(2026, 6, 19, 0, 0);

    @BeforeEach
    void setUp() {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(operator.getId()).thenReturn(10L);
    }

    @Test
    void getSummary_shouldAggregateAllSections() {
        stubSummaryData(null, null, List.<Long>of());

        DashboardSummaryResponse response = dashboardService.getSummary(from, to, null, null);

        assertNotNull(response);
        assertEquals(3L, response.getDeviceSummary().getActive());
        assertEquals(20L, response.getTransactionSummary().getTotal());
        assertEquals(11L, response.getIncidentSummary().getTotal());
        assertEquals(6L, response.getBatchSummary().getTotal());
        assertEquals(4L, response.getControlSyncSummary().getTotal());
    }

    @Test
    void getSummary_shouldUseStationOnlyFilterWhenStationProvided() {
        stubSummaryData(null, 101L, List.of());

        dashboardService.getSummary(from, to, null, 101L);

        verifySummaryRepositories(null, 101L);
    }

    @Test
    void getSummary_shouldUseRouteOnlyFilterWhenRouteProvided() {
        stubSummaryData(1L, null, List.of(101L, 102L));

        dashboardService.getSummary(from, to, 1L, null);

        verifySummaryRepositories(1L, null);
    }

    @Test
    void getSummary_shouldUseRouteAndStationMatchBranch() {
        stubSummaryData(1L, 101L, List.of(101L, 102L));

        dashboardService.getSummary(from, to, 1L, 101L);

        verifySummaryRepositories(1L, 101L);
    }

    @Test
    void getSummary_shouldUseRouteAndStationMismatchBranch() {
        stubSummaryData(1L, 999L, List.of(101L, 102L));

        dashboardService.getSummary(from, to, 1L, 999L);

        verifySummaryRepositories(1L, 999L);
    }

    @Test
    void getSummary_shouldUseRouteOnlyEmptyBranch() {
        stubSummaryData(1L, null, List.<Long>of());

        dashboardService.getSummary(from, to, 1L, null);

        verifySummaryRepositories(1L, null);
    }

    @Test
    void getSummary_shouldRejectInvalidTimeRange() {
        AppException exception = assertThrows(AppException.class,
                () -> dashboardService.getSummary(to, from, null, null));

        assertEquals(ErrorCode.INVALID_DASHBOARD_TIME_RANGE, exception.getErrorCode());
    }

    @Test
    void getSummary_shouldRejectTooWideRange() {
        LocalDateTime wideFrom = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime wideTo = LocalDateTime.of(2026, 6, 19, 0, 0);

        AppException exception = assertThrows(AppException.class,
                () -> dashboardService.getSummary(wideFrom, wideTo, null, null));

        assertEquals(ErrorCode.DASHBOARD_QUERY_TOO_WIDE, exception.getErrorCode());
    }

    @Test
    void getSummary_shouldRejectInvalidRouteId() {
        AppException exception = assertThrows(AppException.class,
                () -> dashboardService.getSummary(from, to, 0L, null));

        assertEquals(ErrorCode.INVALID_ROUTE_ID, exception.getErrorCode());
    }

    @Test
    void getSummary_shouldRejectInvalidStationId() {
        AppException exception = assertThrows(AppException.class,
                () -> dashboardService.getSummary(from, to, null, 0L));

        assertEquals(ErrorCode.INVALID_STATION_ID, exception.getErrorCode());
    }

    @Test
    void getTransactionTimeline_shouldDefaultBucketToHourAndMapRows() {
        List<Object[]> rows = Collections.singletonList(new Object[]{from, 12L, 7L, 3L, 2L});
        when(transactionRepository.getDashboardTransactionTimeline(eq(10L), eq(from), eq(to), isNull(), isNull(), eq("hour")))
                .thenReturn(rows);

        DashboardTransactionTimelineResponse response = dashboardService.getTransactionTimeline(from, to, null, null, null);

        assertEquals("hour", response.getBucket());
        assertEquals(1, response.getItems().size());
        DashboardTransactionTimelineItemResponse item = response.getItems().get(0);
        assertEquals(12L, item.getTotal());
        assertEquals(7L, item.getOpenGate());
        assertEquals(3L, item.getDeny());
        assertEquals(2L, item.getAcceptedForForwarding());
    }

    @Test
    void getTransactionTimeline_shouldAcceptDayBucket() {
        List<Object[]> rows = Collections.singletonList(new Object[]{from, 30L, 20L, 5L, 5L});
        when(transactionRepository.getDashboardTransactionTimeline(eq(10L), eq(from), eq(to), isNull(), isNull(), eq("day")))
                .thenReturn(rows);

        DashboardTransactionTimelineResponse response = dashboardService.getTransactionTimeline(from, to, null, null, "day");

        assertEquals("day", response.getBucket());
        assertEquals(1, response.getItems().size());
    }

    @Test
    void getTransactionTimeline_shouldRejectInvalidBucket() {
        AppException exception = assertThrows(AppException.class,
                () -> dashboardService.getTransactionTimeline(from, to, null, null, "week"));

        assertEquals(ErrorCode.INVALID_DASHBOARD_BUCKET, exception.getErrorCode());
    }

    @Test
    void getTransactionTimeline_shouldUseDefaultRangeWhenMissing() {
        List<Object[]> rows = List.of();
        when(transactionRepository.getDashboardTransactionTimeline(eq(10L), any(LocalDateTime.class), any(LocalDateTime.class), isNull(), isNull(), eq("hour")))
                .thenReturn(rows);

        dashboardService.getTransactionTimeline(null, null, null, null, null);

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(transactionRepository).getDashboardTransactionTimeline(eq(10L), fromCaptor.capture(), toCaptor.capture(), isNull(), isNull(), eq("hour"));

        Duration duration = Duration.between(fromCaptor.getValue(), toCaptor.getValue());
        assertTrue(duration.toHours() >= 23 && duration.toHours() <= 25);
    }

    @Test
    void getTransactionTimeline_shouldUsePrevious24HoursWhenFromMissing() {
        List<Object[]> rows = List.of();
        when(transactionRepository.getDashboardTransactionTimeline(eq(10L), any(LocalDateTime.class), eq(to), isNull(), isNull(), eq("hour")))
                .thenReturn(rows);

        dashboardService.getTransactionTimeline(null, to, null, null, null);

        verify(transactionRepository).getDashboardTransactionTimeline(eq(10L), any(LocalDateTime.class), eq(to), isNull(), isNull(), eq("hour"));
    }

    @Test
    void getTransactionTimeline_shouldUseNowWhenToMissing() {
        List<Object[]> rows = List.of();
        when(transactionRepository.getDashboardTransactionTimeline(eq(10L), eq(from), any(LocalDateTime.class), isNull(), isNull(), eq("hour")))
                .thenReturn(rows);

        dashboardService.getTransactionTimeline(from, null, null, null, null);

        verify(transactionRepository).getDashboardTransactionTimeline(eq(10L), eq(from), any(LocalDateTime.class), isNull(), isNull(), eq("hour"));
    }

    @Test
    void getRouteStationSummaries_shouldMapRows() {
        List<Object[]> rows = Collections.singletonList(new Object[]{1L, "R1", "Route 1", 101L, "S1", "Station 1", 20L, 12L, 8L});
        when(transactionRepository.getDashboardRouteStationSummaries(eq(10L), eq(from), eq(to), isNull(), isNull()))
                .thenReturn(rows);

        DashboardRouteStationSummaryResponse response = dashboardService.getRouteStationSummaries(from, to, null, null);

        assertEquals(1, response.getItems().size());
        DashboardRouteStationSummaryItemResponse item = response.getItems().get(0);
        assertEquals("R1", item.getRouteCode());
        assertEquals("S1", item.getStationCode());
        assertEquals(20L, item.getTotal());
    }

    @Test
    void getRecentIncidents_shouldMapRowsAndStationInfo() {
        DeviceIncident incident = mock(DeviceIncident.class);
        when(incident.getId()).thenReturn("incident-1");
        when(incident.getOccurredAt()).thenReturn(from);
        when(incident.getStationId()).thenReturn(101L);
        when(incident.getDeviceId()).thenReturn(201L);
        when(incident.getDeviceCode()).thenReturn("DEV-1");
        when(incident.getSeverity()).thenReturn("HIGH");
        when(incident.getIncidentType()).thenReturn("POWER");
        when(incident.getResolvedAt()).thenReturn(null);

        Station station = mock(Station.class);
        when(station.getId()).thenReturn(101L);
        when(station.getStationCode()).thenReturn("ST-1");
        when(station.getStationName()).thenReturn("Station 1");

        when(mongoTemplate.find(any(Query.class), eq(DeviceIncident.class))).thenReturn(List.of(incident));
        when(stationRepository.findAllById(any(Set.class))).thenReturn(List.of(station));

        DashboardRecentIncidentResponse response = dashboardService.getRecentIncidents(from, to, null, null, null, 10);

        assertEquals(1, response.getItems().size());
        DashboardRecentIncidentItemResponse item = response.getItems().get(0);
        assertEquals("incident-1", item.getIncidentId());
        assertEquals("ST-1", item.getStationCode());
        assertFalse(item.isResolved());
    }

    @Test
    void getRecentIncidents_shouldUseRouteOnlyEmptyBranch() {
        when(stationRepository.findIdsByRouteId(1L)).thenReturn(List.of());
        when(mongoTemplate.find(any(Query.class), eq(DeviceIncident.class))).thenReturn(List.of());

        DashboardRecentIncidentResponse response = dashboardService.getRecentIncidents(from, to, 1L, null, null, 10);

        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void getRecentIncidents_shouldUseRouteAndStationMismatchBranch() {
        when(stationRepository.findIdsByRouteId(1L)).thenReturn(List.of(101L));
        when(mongoTemplate.find(any(Query.class), eq(DeviceIncident.class))).thenReturn(List.of());

        DashboardRecentIncidentResponse response = dashboardService.getRecentIncidents(from, to, 1L, 999L, null, 10);

        assertTrue(response.getItems().isEmpty());
    }

    @Test
    void getRecentIncidents_shouldRejectInvalidLimit() {
        AppException exception = assertThrows(AppException.class,
                () -> dashboardService.getRecentIncidents(from, to, null, null, null, 0));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void getAlerts_shouldCreateAlertsFromSummaryAndRecentIncidents() {
        DashboardSummaryResponse summary = DashboardSummaryResponse.builder()
                .deviceSummary(DashboardDeviceSummaryResponse.builder().active(1).offline(2).maintenance(3).disabled(4).build())
                .transactionSummary(DashboardTransactionSummaryResponse.builder().total(10).openGate(6).deny(4).acceptedForForwarding(0).denyRate(40.0).build())
                .incidentSummary(DashboardIncidentSummaryResponse.builder().total(5).open(1).high(2).build())
                .batchSummary(DashboardBatchSummaryResponse.builder().total(3).created(1).submitted(1).accepted(0).rejected(0).failed(1).build())
                .controlSyncSummary(DashboardControlSyncSummaryResponse.builder().total(2).pending(0).applied(1).failed(1).build())
                .build();

        DashboardRecentIncidentResponse recentIncidents = DashboardRecentIncidentResponse.builder()
                .items(List.of(DashboardRecentIncidentItemResponse.builder()
                        .incidentId("incident-9")
                        .occurredAt(from)
                        .stationId(101L)
                        .stationCode("ST-9")
                        .deviceId(201L)
                        .deviceCode("DEV-9")
                        .severity("HIGH")
                        .incidentType("POWER")
                        .resolved(false)
                        .build()))
                .build();

        DashboardService spyService = org.mockito.Mockito.spy(dashboardService);
        doReturn(summary).when(spyService).getSummary(eq(from), eq(to), isNull(), isNull());
        doReturn(recentIncidents).when(spyService).getRecentIncidents(eq(from), eq(to), isNull(), isNull(), isNull(), eq(10));

        DashboardAlertResponse response = spyService.getAlerts(from, to, null, null, 10);

        assertEquals(5, response.getItems().size());
        assertTrue(response.getItems().stream().anyMatch(item -> "DEVICE_OFFLINE".equals(item.getType())));
        assertTrue(response.getItems().stream().anyMatch(item -> "RECENT_INCIDENT".equals(item.getType())));
    }

    private void stubSummaryData(Long routeId, Long stationId, List<Long> stationIdsInRoute) {
        when(deviceRepository.getDashboardDeviceSummary(eq(10L), eq(routeId), eq(stationId)))
                .thenReturn(List.<Object[]>of(new Object[]{3L, 2L, 1L, 4L}));
        when(transactionRepository.getDashboardTransactionSummary(eq(10L), eq(from), eq(to), eq(routeId), eq(stationId)))
                .thenReturn(List.<Object[]>of(new Object[]{20L, 12L, 5L, 3L}));
        when(batchRepository.getDashboardBatchSummary(eq(10L), eq(from), eq(to)))
                .thenReturn(List.<Object[]>of(new Object[]{6L, 1L, 2L, 1L, 1L, 1L}));
        when(stationControlSyncRepository.getDashboardControlSyncSummary(eq(10L), eq(from), eq(to), eq(routeId), eq(stationId)))
                .thenReturn(List.<Object[]>of(new Object[]{4L, 1L, 2L, 1L}));
        when(mongoTemplate.count(any(Query.class), eq(DeviceIncident.class)))
                .thenReturn(11L, 2L, 1L);
        when(stationRepository.findIdsByRouteId(eq(routeId == null ? 0L : routeId)))
                .thenReturn(stationIdsInRoute);
    }

    private void verifySummaryRepositories(Long routeId, Long stationId) {
        if (routeId == null) {
            verify(deviceRepository).getDashboardDeviceSummary(eq(10L), isNull(), eq(stationId));
            verify(transactionRepository).getDashboardTransactionSummary(eq(10L), eq(from), eq(to), isNull(), eq(stationId));
            verify(stationControlSyncRepository).getDashboardControlSyncSummary(eq(10L), eq(from), eq(to), isNull(), eq(stationId));
        } else {
            verify(deviceRepository).getDashboardDeviceSummary(eq(10L), eq(routeId), eq(stationId));
            verify(transactionRepository).getDashboardTransactionSummary(eq(10L), eq(from), eq(to), eq(routeId), eq(stationId));
            verify(stationControlSyncRepository).getDashboardControlSyncSummary(eq(10L), eq(from), eq(to), eq(routeId), eq(stationId));
        }
        verify(batchRepository).getDashboardBatchSummary(eq(10L), eq(from), eq(to));
    }
}
