package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.device.SubmitHeartbeatRequest;
import com.vdt.afc_ops_service.dto.request.device.SubmitIncidentRequest;
import com.vdt.afc_ops_service.dto.response.device.SubmitHeartbeatResponse;
import com.vdt.afc_ops_service.dto.response.device.SubmitIncidentResponse;

public interface IDeviceIntegrationService {
    SubmitHeartbeatResponse submitHeartbeat(String deviceCode, String deviceSecret, SubmitHeartbeatRequest request);
    SubmitIncidentResponse submitIncident(String deviceCode, String deviceSecret, SubmitIncidentRequest request);
}
