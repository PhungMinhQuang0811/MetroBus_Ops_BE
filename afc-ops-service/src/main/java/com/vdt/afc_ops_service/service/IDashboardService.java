package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.response.dashboard.DashboardAlertResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRecentIncidentResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRouteStationSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardTransactionTimelineResponse;

import java.time.LocalDateTime;

public interface IDashboardService {
    DashboardSummaryResponse getSummary(LocalDateTime from, LocalDateTime to, Long routeId, Long stationId);

    DashboardTransactionTimelineResponse getTransactionTimeline(LocalDateTime from, LocalDateTime to,
                                                                Long routeId, Long stationId, String bucket);

    DashboardRouteStationSummaryResponse getRouteStationSummaries(LocalDateTime from, LocalDateTime to,
                                                                  Long routeId, Long stationId);

    DashboardRecentIncidentResponse getRecentIncidents(LocalDateTime from, LocalDateTime to,
                                                       Long routeId, Long stationId, String severity, int limit);

    DashboardAlertResponse getAlerts(LocalDateTime from, LocalDateTime to,
                                     Long routeId, Long stationId, int limit);
}
