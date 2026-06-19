package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardAlertResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRecentIncidentResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardRouteStationSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardSummaryResponse;
import com.vdt.afc_ops_service.dto.response.dashboard.DashboardTransactionTimelineResponse;
import com.vdt.afc_ops_service.service.IDashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardController {

    IDashboardService dashboardService;

    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long stationId
    ) {
        return ApiResponse.<DashboardSummaryResponse>builder()
                .result(dashboardService.getSummary(from, to, routeId, stationId))
                .build();
    }

    @GetMapping("/transaction-timeline")
    public ApiResponse<DashboardTransactionTimelineResponse> getTransactionTimeline(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long stationId,
            @RequestParam(defaultValue = "hour") String bucket
    ) {
        return ApiResponse.<DashboardTransactionTimelineResponse>builder()
                .result(dashboardService.getTransactionTimeline(from, to, routeId, stationId, bucket))
                .build();
    }

    @GetMapping("/route-station-summaries")
    public ApiResponse<DashboardRouteStationSummaryResponse> getRouteStationSummaries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long stationId
    ) {
        return ApiResponse.<DashboardRouteStationSummaryResponse>builder()
                .result(dashboardService.getRouteStationSummaries(from, to, routeId, stationId))
                .build();
    }

    @GetMapping("/recent-incidents")
    public ApiResponse<DashboardRecentIncidentResponse> getRecentIncidents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) String severity,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.<DashboardRecentIncidentResponse>builder()
                .result(dashboardService.getRecentIncidents(from, to, routeId, stationId, severity, limit))
                .build();
    }

    @GetMapping("/alerts")
    public ApiResponse<DashboardAlertResponse> getAlerts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long stationId,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.<DashboardAlertResponse>builder()
                .result(dashboardService.getAlerts(from, to, routeId, stationId, limit))
                .build();
    }
}
