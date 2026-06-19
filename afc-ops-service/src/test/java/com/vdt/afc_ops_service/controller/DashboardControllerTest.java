package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.response.dashboard.DashboardAlertResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRecentIncidentResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRouteStationSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardTransactionTimelineResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardAlertItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardRecentIncidentItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardRouteStationSummaryItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.item.DashboardTransactionTimelineItemResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardBatchSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardControlSyncSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardDeviceSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardIncidentSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.summary.DashboardTransactionSummaryResponse;
import com.vdt.afc_ops_service.service.IDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private IDashboardService dashboardService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DashboardController(dashboardService)).build();
    }

    @Test
    void getSummary_shouldReturnOk() throws Exception {
        when(dashboardService.getSummary(isNull(), isNull(), isNull(), isNull())).thenReturn(sampleSummary());

        mockMvc.perform(get("/dashboard/summary"))
                .andExpect(status().isOk());

        verify(dashboardService).getSummary(null, null, null, null);
    }

    @Test
    void getTransactionTimeline_shouldReturnOk() throws Exception {
        when(dashboardService.getTransactionTimeline(isNull(), isNull(), isNull(), isNull(), any()))
                .thenReturn(DashboardTransactionTimelineResponse.builder()
                        .bucket("hour")
                        .items(List.of(DashboardTransactionTimelineItemResponse.builder()
                                .timePoint(LocalDateTime.of(2026, 6, 19, 10, 0))
                                .total(1L)
                                .openGate(1L)
                                .deny(0L)
                                .acceptedForForwarding(0L)
                                .build()))
                        .build());

        mockMvc.perform(get("/dashboard/transaction-timeline").param("bucket", "day"))
                .andExpect(status().isOk());

        verify(dashboardService).getTransactionTimeline(null, null, null, null, "day");
    }

    @Test
    void getRouteStationSummaries_shouldReturnOk() throws Exception {
        when(dashboardService.getRouteStationSummaries(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(DashboardRouteStationSummaryResponse.builder()
                        .items(List.of(DashboardRouteStationSummaryItemResponse.builder()
                                .routeId(1L)
                                .routeCode("R1")
                                .routeName("Route 1")
                                .stationId(101L)
                                .stationCode("S1")
                                .stationName("Station 1")
                                .total(10L)
                                .openGate(6L)
                                .deny(4L)
                                .build()))
                        .build());

        mockMvc.perform(get("/dashboard/route-station-summaries"))
                .andExpect(status().isOk());

        verify(dashboardService).getRouteStationSummaries(null, null, null, null);
    }

    @Test
    void getRecentIncidents_shouldReturnOk() throws Exception {
        when(dashboardService.getRecentIncidents(isNull(), isNull(), isNull(), isNull(), eq("HIGH"), eq(10)))
                .thenReturn(DashboardRecentIncidentResponse.builder()
                        .items(List.of(DashboardRecentIncidentItemResponse.builder()
                                .incidentId("incident-1")
                                .occurredAt(LocalDateTime.of(2026, 6, 19, 10, 0))
                                .stationId(101L)
                                .stationCode("S1")
                                .deviceId(201L)
                                .deviceCode("D1")
                                .severity("HIGH")
                                .incidentType("POWER")
                                .resolved(false)
                                .build()))
                        .build());

        mockMvc.perform(get("/dashboard/recent-incidents")
                        .param("severity", "HIGH")
                        .param("limit", "10"))
                .andExpect(status().isOk());

        verify(dashboardService).getRecentIncidents(isNull(), isNull(), isNull(), isNull(), eq("HIGH"), eq(10));
    }

    @Test
    void getAlerts_shouldReturnOk() throws Exception {
        when(dashboardService.getAlerts(isNull(), isNull(), isNull(), isNull(), anyInt()))
                .thenReturn(DashboardAlertResponse.builder()
                        .items(List.of(DashboardAlertItemResponse.builder()
                                .type("DEVICE_OFFLINE")
                                .severity("HIGH")
                                .message("1 devices are offline")
                                .resourceType("DEVICE")
                                .resourceId(null)
                                .build()))
                        .build());

        mockMvc.perform(get("/dashboard/alerts").param("limit", "10"))
                .andExpect(status().isOk());

        verify(dashboardService).getAlerts(isNull(), isNull(), isNull(), isNull(), eq(10));
    }

    private DashboardSummaryResponse sampleSummary() {
        return DashboardSummaryResponse.builder()
                .deviceSummary(DashboardDeviceSummaryResponse.builder().active(1).offline(0).maintenance(0).disabled(0).build())
                .transactionSummary(DashboardTransactionSummaryResponse.builder().total(1).openGate(1).deny(0).acceptedForForwarding(0).denyRate(0.0).build())
                .incidentSummary(DashboardIncidentSummaryResponse.builder().total(1).open(0).high(0).build())
                .batchSummary(DashboardBatchSummaryResponse.builder().total(1).created(0).submitted(0).accepted(0).rejected(0).failed(0).build())
                .controlSyncSummary(DashboardControlSyncSummaryResponse.builder().total(1).pending(0).applied(1).failed(0).build())
                .build();
    }
}
