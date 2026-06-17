package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.device.SubmitHeartbeatRequest;
import com.vdt.afc_ops_service.dto.request.device.SubmitIncidentRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceHeartbeatHistoryResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceStatusResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentDetailResponse;
import com.vdt.afc_ops_service.dto.response.device.SubmitHeartbeatResponse;
import com.vdt.afc_ops_service.dto.response.device.SubmitIncidentResponse;
import com.vdt.afc_ops_service.service.IDeviceIntegrationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/afc-ops")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AfcOpsDeviceController {

    IDeviceIntegrationService deviceIntegrationService;

    @PostMapping("/submit-heartbeat")
    public ApiResponse<SubmitHeartbeatResponse> submitHeartbeat(
            @RequestHeader("X-Device-Code") String deviceCode,
            @RequestHeader("X-Device-Secret") String deviceSecret,
            @Valid @RequestBody SubmitHeartbeatRequest request
    ) {
        return ApiResponse.<SubmitHeartbeatResponse>builder()
                .result(deviceIntegrationService.submitHeartbeat(deviceCode, deviceSecret, request))
                .build();
    }

    @PostMapping("/submit-device-incident")
    public ApiResponse<SubmitIncidentResponse> submitIncident(
            @RequestHeader("X-Device-Code") String deviceCode,
            @RequestHeader("X-Device-Secret") String deviceSecret,
            @Valid @RequestBody SubmitIncidentRequest request
    ) {
        return ApiResponse.<SubmitIncidentResponse>builder()
                .result(deviceIntegrationService.submitIncident(deviceCode, deviceSecret, request))
                .build();
    }

    @GetMapping("/get-device-status")
    public ApiResponse<PageResponse<DeviceStatusResponse>> getDeviceStatus(
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<DeviceStatusResponse>>builder()
                .result(deviceIntegrationService.getDeviceStatus(routeId, stationId, status, page, size))
                .build();
    }

    @GetMapping("/get-device-heartbeats")
    public ApiResponse<PageResponse<DeviceHeartbeatHistoryResponse>> getDeviceHeartbeats(
            @RequestParam Long deviceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<DeviceHeartbeatHistoryResponse>>builder()
                .result(deviceIntegrationService.getDeviceHeartbeats(deviceId, page, size))
                .build();
    }

    @GetMapping("/search-incidents")
    public ApiResponse<PageResponse<DeviceIncidentResponse>> searchIncidents(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String incidentType,
            @RequestParam(required = false) Boolean resolved,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<DeviceIncidentResponse>>builder()
                .result(deviceIntegrationService.searchIncidents(from, to, stationId, deviceId, severity, incidentType, resolved, page, size))
                .build();
    }

    @GetMapping("/get-incident/{incidentId}")
    public ApiResponse<DeviceIncidentDetailResponse> getIncidentDetail(
            @PathVariable String incidentId
    ) {
        return ApiResponse.<DeviceIncidentDetailResponse>builder()
                .result(deviceIntegrationService.getIncidentDetail(incidentId))
                .build();
    }
}
