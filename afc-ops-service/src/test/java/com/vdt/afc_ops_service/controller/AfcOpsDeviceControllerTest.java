package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.device.SubmitHeartbeatRequest;
import com.vdt.afc_ops_service.dto.request.device.SubmitIncidentRequest;
import com.vdt.afc_ops_service.dto.response.device.SubmitHeartbeatResponse;
import com.vdt.afc_ops_service.dto.response.device.SubmitIncidentResponse;
import com.vdt.afc_ops_service.service.IDeviceIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AfcOpsDeviceControllerTest {

    @Mock
    IDeviceIntegrationService deviceIntegrationService;

    MockMvc mockMvc;
    AfcOpsDeviceController afcOpsDeviceController;

    @BeforeEach
    void setUp() {
        afcOpsDeviceController = new AfcOpsDeviceController(deviceIntegrationService);
        mockMvc = MockMvcBuilders.standaloneSetup(afcOpsDeviceController).build();
    }

    @Test
    void submitHeartbeat_ValidRequest_ReturnsSuccess() throws Exception {
        SubmitHeartbeatResponse mockResponse = SubmitHeartbeatResponse.builder()
                .deviceCode("METRO-001-ST-001-DV-001")
                .accepted(true)
                .serverTime(LocalDateTime.of(2026, 6, 16, 18, 0, 0))
                .build();

        when(deviceIntegrationService.submitHeartbeat(
                eq("METRO-001-ST-001-DV-001"), eq("device-secret"), any(SubmitHeartbeatRequest.class)
        )).thenReturn(mockResponse);

        mockMvc.perform(post("/afc-ops/submit-heartbeat")
                        .header("X-Device-Code", "METRO-001-ST-001-DV-001")
                        .header("X-Device-Secret", "device-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sentAt": "2026-06-16T18:00:00",
                                  "status": "ONLINE",
                                  "firmwareVersion": "1.0.1",
                                  "metrics": {"cpu": 0.15}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.deviceCode").value("METRO-001-ST-001-DV-001"))
                .andExpect(jsonPath("$.result.accepted").value(true));
    }

    @Test
    void submitIncident_ValidRequest_ReturnsSuccess() throws Exception {
        SubmitIncidentResponse mockResponse = SubmitIncidentResponse.builder()
                .accepted(true)
                .incidentId("mongo-incident-id")
                .build();

        when(deviceIntegrationService.submitIncident(
                eq("METRO-001-ST-001-DV-001"), eq("device-secret"), any(SubmitIncidentRequest.class)
        )).thenReturn(mockResponse);

        mockMvc.perform(post("/afc-ops/submit-device-incident")
                        .header("X-Device-Code", "METRO-001-ST-001-DV-001")
                        .header("X-Device-Secret", "device-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentType": "GATE_JAMMED",
                                  "severity": "CRITICAL",
                                  "occurredAt": "2026-06-16T17:59:00",
                                  "message": "Physical barrier jammed",
                                  "payload": {"error": "MOTOR_JAMMED"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.accepted").value(true))
                .andExpect(jsonPath("$.result.incidentId").value("mongo-incident-id"));
    }
}
