package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.device.SubmitHeartbeatRequest;
import com.vdt.afc_ops_service.dto.request.device.SubmitIncidentRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.device.SubmitHeartbeatResponse;
import com.vdt.afc_ops_service.dto.response.device.SubmitIncidentResponse;
import com.vdt.afc_ops_service.service.IDeviceIntegrationService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
