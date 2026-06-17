package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.device.SubmitHeartbeatRequest;
import com.vdt.afc_ops_service.dto.request.device.SubmitIncidentRequest;
import com.vdt.afc_ops_service.dto.response.device.SubmitHeartbeatResponse;
import com.vdt.afc_ops_service.dto.response.device.SubmitIncidentResponse;

import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceHeartbeatHistoryResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceStatusResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentDetailResponse;
import java.time.LocalDateTime;

public interface IDeviceIntegrationService {
    SubmitHeartbeatResponse submitHeartbeat(String deviceCode, String deviceSecret, SubmitHeartbeatRequest request);
    SubmitIncidentResponse submitIncident(String deviceCode, String deviceSecret, SubmitIncidentRequest request);
    PageResponse<DeviceStatusResponse> getDeviceStatus(Long routeId, Long stationId, String status, int page, int size);
    PageResponse<DeviceHeartbeatHistoryResponse> getDeviceHeartbeats(Long deviceId, int page, int size);
    PageResponse<DeviceIncidentResponse> searchIncidents(
            LocalDateTime from, LocalDateTime to, Long stationId, Long deviceId,
            String severity, String incidentType, Boolean resolved, int page, int size);
    DeviceIncidentDetailResponse getIncidentDetail(String incidentId);
}
