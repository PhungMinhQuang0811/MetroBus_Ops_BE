package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.device.SubmitHeartbeatRequest;
import com.vdt.afc_ops_service.dto.request.device.SubmitIncidentRequest;
import com.vdt.afc_ops_service.dto.response.device.SubmitHeartbeatResponse;
import com.vdt.afc_ops_service.dto.response.device.SubmitIncidentResponse;
import com.vdt.afc_ops_service.service.IDeviceIntegrationService;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceHeartbeatHistoryResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceStatusResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentDetailResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void getDeviceStatus_ValidRequest_ReturnsSuccess() throws Exception {
        DeviceStatusResponse mockItem = DeviceStatusResponse.builder()
                .deviceId(10L)
                .deviceCode("QR-BT-001")
                .stationId(1L)
                .stationName("Ben Thanh")
                .deviceType("QR_SCANNER_SIMULATOR")
                .status("ACTIVE")
                .lastSeenAt(LocalDateTime.of(2026, 6, 16, 18, 0, 0))
                .offlineSeconds(5L)
                .build();

        PageResponse<DeviceStatusResponse> mockResponse = PageResponse.<DeviceStatusResponse>builder()
                .items(List.of(mockItem))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(deviceIntegrationService.getDeviceStatus(eq(1L), eq(2L), eq("ACTIVE"), eq(0), eq(20)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/afc-ops/get-device-status")
                        .param("routeId", "1")
                        .param("stationId", "2")
                        .param("status", "ACTIVE")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].deviceId").value(10))
                .andExpect(jsonPath("$.result.items[0].deviceCode").value("QR-BT-001"))
                .andExpect(jsonPath("$.result.items[0].offlineSeconds").value(5));
    }

    @Test
    void getDeviceHeartbeats_ValidRequest_ReturnsSuccess() throws Exception {
        DeviceHeartbeatHistoryResponse mockItem = DeviceHeartbeatHistoryResponse.builder()
                .id("mongo-heartbeat-id")
                .deviceId(10L)
                .deviceCode("QR-BT-001")
                .stationId(1L)
                .status("ACTIVE")
                .firmwareVersion("1.0.7")
                .sentAt(LocalDateTime.of(2026, 6, 16, 18, 0, 0))
                .receivedAt(LocalDateTime.of(2026, 6, 16, 18, 0, 5))
                .payload(Map.of("cpuUsage", 12.5))
                .build();

        PageResponse<DeviceHeartbeatHistoryResponse> mockResponse = PageResponse.<DeviceHeartbeatHistoryResponse>builder()
                .items(List.of(mockItem))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(deviceIntegrationService.getDeviceHeartbeats(eq(10L), eq(0), eq(20)))
                .thenReturn(mockResponse);

        mockMvc.perform(get("/afc-ops/get-device-heartbeats")
                        .param("deviceId", "10")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].id").value("mongo-heartbeat-id"))
                .andExpect(jsonPath("$.result.items[0].deviceId").value(10))
                .andExpect(jsonPath("$.result.items[0].status").value("ACTIVE"));
    }

    @Test
    void searchIncidents_ValidRequest_ReturnsSuccess() throws Exception {
        DeviceIncidentResponse mockItem = DeviceIncidentResponse.builder()
                .id("mongo-incident-id")
                .deviceId(10L)
                .deviceCode("QR-BT-001")
                .stationId(1L)
                .incidentType("GATE_JAMMED")
                .severity("HIGH")
                .message("Gate arm jammed")
                .occurredAt(LocalDateTime.of(2026, 6, 16, 18, 0, 0))
                .receivedAt(LocalDateTime.of(2026, 6, 16, 18, 0, 2))
                .resolvedAt(null)
                .build();

        PageResponse<DeviceIncidentResponse> mockResponse = PageResponse.<DeviceIncidentResponse>builder()
                .items(List.of(mockItem))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();

        when(deviceIntegrationService.searchIncidents(
                any(LocalDateTime.class), any(LocalDateTime.class), eq(1L), eq(10L), eq("HIGH"), eq("GATE_JAMMED"), eq(false), eq(0), eq(20)
        )).thenReturn(mockResponse);

        mockMvc.perform(get("/afc-ops/search-incidents")
                        .param("from", "2026-06-16T00:00:00")
                        .param("to", "2026-06-16T23:59:59")
                        .param("stationId", "1")
                        .param("deviceId", "10")
                        .param("severity", "HIGH")
                        .param("incidentType", "GATE_JAMMED")
                        .param("resolved", "false")
                        .param("page", "0")
                        .param("size", "20")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].id").value("mongo-incident-id"))
                .andExpect(jsonPath("$.result.items[0].deviceId").value(10))
                .andExpect(jsonPath("$.result.items[0].severity").value("HIGH"));
    }

    @Test
    void getIncidentDetail_ValidRequest_ReturnsSuccess() throws Exception {
        DeviceIncidentDetailResponse mockResponse = DeviceIncidentDetailResponse.builder()
                .id("mongo-incident-id")
                .deviceId(10L)
                .deviceCode("QR-BT-001")
                .deviceType("QR_SCANNER_SIMULATOR")
                .deviceStatus("ACTIVE")
                .stationId(1L)
                .stationCode("ST-001")
                .stationName("Ben Thanh")
                .routeId(2L)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .incidentType("GATE_JAMMED")
                .severity("HIGH")
                .message("Gate arm jammed")
                .occurredAt(LocalDateTime.of(2026, 6, 16, 18, 0, 0))
                .receivedAt(LocalDateTime.of(2026, 6, 16, 18, 0, 2))
                .resolvedAt(null)
                .payload(Map.of("barrierErrorCode", "ERR_BARRIER_MOTOR_OVERHEAT"))
                .build();

        when(deviceIntegrationService.getIncidentDetail("mongo-incident-id")).thenReturn(mockResponse);

        mockMvc.perform(get("/afc-ops/get-incident/{incidentId}", "mongo-incident-id")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("mongo-incident-id"))
                .andExpect(jsonPath("$.result.deviceId").value(10))
                .andExpect(jsonPath("$.result.routeCode").value("METRO-001"))
                .andExpect(jsonPath("$.result.payload.barrierErrorCode").value("ERR_BARRIER_MOTOR_OVERHEAT"));
    }
}
