package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.document.DeviceHeartbeat;
import com.vdt.afc_ops_service.document.DeviceIncident;
import com.vdt.afc_ops_service.dto.request.device.SubmitHeartbeatRequest;
import com.vdt.afc_ops_service.dto.request.device.SubmitIncidentRequest;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.mongo.DeviceHeartbeatRepository;
import com.vdt.afc_ops_service.repository.mongo.DeviceIncidentRepository;
import com.vdt.afc_ops_service.service.Impl.DeviceIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceIntegrationServiceTest {

    @Mock
    DeviceRepository deviceRepository;

    @Mock
    DeviceHeartbeatRepository deviceHeartbeatRepository;

    @Mock
    DeviceIncidentRepository deviceIncidentRepository;

    DeviceIntegrationService deviceIntegrationService;
    Device device;
    Station station;

    @BeforeEach
    void setUp() {
        deviceIntegrationService = new DeviceIntegrationService(
                deviceRepository,
                deviceHeartbeatRepository,
                deviceIncidentRepository
        );

        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        Route route = Route.builder().id(10L).operator(operator).routeCode("METRO-001").build();
        station = Station.builder()
                .id(100L)
                .route(route)
                .stationCode("METRO-001-ST-001")
                .stationName("Ben Thanh")
                .build();

        device = Device.builder()
                .id(200L)
                .station(station)
                .deviceCode("METRO-001-ST-001-DV-001")
                .deviceType("QR_SCANNER_SIMULATOR")
                .direction("ENTRY")
                .status("ACTIVE")
                .deviceSecret("valid-secret")
                .firmwareVersion("1.0.0")
                .build();
    }

    @Test
    void submitHeartbeat_ValidCredentials_SavesToMongoAndUpdatesDevice() {
        when(deviceRepository.findByDeviceCode("METRO-001-ST-001-DV-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);

        LocalDateTime sentAt = LocalDateTime.of(2026, 6, 16, 17, 0, 0);
        SubmitHeartbeatRequest request = SubmitHeartbeatRequest.builder()
                .sentAt(sentAt)
                .status("ACTIVE")
                .firmwareVersion("1.0.1")
                .metrics(Map.of("cpu", 0.25, "memory", 0.45))
                .build();

        var response = deviceIntegrationService.submitHeartbeat(
                "METRO-001-ST-001-DV-001", "valid-secret", request
        );

        assertTrue(response.isAccepted());
        assertEquals("METRO-001-ST-001-DV-001", response.getDeviceCode());
        assertNotNull(response.getServerTime());

        // Verify device updated in Postgres
        verify(deviceRepository).save(device);
        assertEquals("1.0.1", device.getFirmwareVersion());
        assertNotNull(device.getLastSeenAt());

        // Verify heartbeat saved in Mongo
        ArgumentCaptor<DeviceHeartbeat> heartbeatCaptor = ArgumentCaptor.forClass(DeviceHeartbeat.class);
        verify(deviceHeartbeatRepository).save(heartbeatCaptor.capture());
        DeviceHeartbeat savedHeartbeat = heartbeatCaptor.getValue();
        assertEquals(200L, savedHeartbeat.getDeviceId());
        assertEquals("METRO-001-ST-001-DV-001", savedHeartbeat.getDeviceCode());
        assertEquals(100L, savedHeartbeat.getStationId());
        assertEquals("ACTIVE", savedHeartbeat.getStatus());
        assertEquals("1.0.1", savedHeartbeat.getFirmwareVersion());
        assertEquals(sentAt, savedHeartbeat.getSentAt());
        assertNotNull(savedHeartbeat.getReceivedAt());
        assertEquals(0.25, savedHeartbeat.getPayload().get("cpu"));
    }

    @Test
    void submitHeartbeat_InvalidSecret_ThrowsDeviceAuthFailed() {
        when(deviceRepository.findByDeviceCode("METRO-001-ST-001-DV-001")).thenReturn(Optional.of(device));

        SubmitHeartbeatRequest request = SubmitHeartbeatRequest.builder()
                .sentAt(LocalDateTime.now())
                .status("ACTIVE")
                .firmwareVersion("1.0.1")
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.submitHeartbeat("METRO-001-ST-001-DV-001", "wrong-secret", request)
        );

        assertEquals(ErrorCode.DEVICE_AUTH_FAILED, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
        verify(deviceHeartbeatRepository, never()).save(any());
    }

    @Test
    void submitHeartbeat_DeviceNotFound_ThrowsDeviceNotFound() {
        when(deviceRepository.findByDeviceCode("UNKNOWN-DEVICE")).thenReturn(Optional.empty());

        SubmitHeartbeatRequest request = SubmitHeartbeatRequest.builder()
                .sentAt(LocalDateTime.now())
                .status("ACTIVE")
                .firmwareVersion("1.0.1")
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.submitHeartbeat("UNKNOWN-DEVICE", "secret", request)
        );

        assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
        verify(deviceHeartbeatRepository, never()).save(any());
    }

    @Test
    void submitIncident_ValidCredentials_SavesToMongo() {
        when(deviceRepository.findByDeviceCode("METRO-001-ST-001-DV-001")).thenReturn(Optional.of(device));
        
        DeviceIncident savedMongoIncident = DeviceIncident.builder()
                .id("mongo-incident-id")
                .build();
        when(deviceIncidentRepository.save(any(DeviceIncident.class))).thenReturn(savedMongoIncident);

        LocalDateTime occurredAt = LocalDateTime.of(2026, 6, 16, 17, 10, 0);
        SubmitIncidentRequest request = SubmitIncidentRequest.builder()
                .incidentType("GATE_JAMMED")
                .severity("HIGH")
                .occurredAt(occurredAt)
                .message("Gate arm jammed")
                .payload(Map.of("sensor", "ARM_LOCK"))
                .build();

        var response = deviceIntegrationService.submitIncident(
                "METRO-001-ST-001-DV-001", "valid-secret", request
        );

        assertTrue(response.isAccepted());
        assertEquals("mongo-incident-id", response.getIncidentId());

        // Verify incident saved in Mongo
        ArgumentCaptor<DeviceIncident> incidentCaptor = ArgumentCaptor.forClass(DeviceIncident.class);
        verify(deviceIncidentRepository).save(incidentCaptor.capture());
        DeviceIncident savedIncident = incidentCaptor.getValue();
        assertEquals(200L, savedIncident.getDeviceId());
        assertEquals("METRO-001-ST-001-DV-001", savedIncident.getDeviceCode());
        assertEquals(100L, savedIncident.getStationId());
        assertEquals("GATE_JAMMED", savedIncident.getIncidentType());
        assertEquals("HIGH", savedIncident.getSeverity());
        assertEquals("Gate arm jammed", savedIncident.getMessage());
        assertEquals(occurredAt, savedIncident.getOccurredAt());
        assertNotNull(savedIncident.getReceivedAt());
        assertEquals("ARM_LOCK", savedIncident.getPayload().get("sensor"));
    }

    @Test
    void submitIncident_InvalidSecret_ThrowsDeviceAuthFailed() {
        when(deviceRepository.findByDeviceCode("METRO-001-ST-001-DV-001")).thenReturn(Optional.of(device));

        SubmitIncidentRequest request = SubmitIncidentRequest.builder()
                .incidentType("GATE_JAMMED")
                .severity("HIGH")
                .occurredAt(LocalDateTime.now())
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.submitIncident("METRO-001-ST-001-DV-001", "wrong-secret", request)
        );

        assertEquals(ErrorCode.DEVICE_AUTH_FAILED, exception.getErrorCode());
        verify(deviceIncidentRepository, never()).save(any());
    }

    @Test
    void submitHeartbeat_StatusMaintenance_DoesNotChangePostgresStatus() {
        device.setStatus("MAINTENANCE");
        when(deviceRepository.findByDeviceCode("METRO-001-ST-001-DV-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);

        SubmitHeartbeatRequest request = SubmitHeartbeatRequest.builder()
                .sentAt(LocalDateTime.now())
                .status("ACTIVE")
                .firmwareVersion("1.0.1")
                .build();

        var response = deviceIntegrationService.submitHeartbeat(
                "METRO-001-ST-001-DV-001", "valid-secret", request
        );

        assertTrue(response.isAccepted());
        verify(deviceRepository).save(device);
        assertEquals("MAINTENANCE", device.getStatus());
    }

    @Test
    void submitHeartbeat_StatusDisabled_DoesNotChangePostgresStatus() {
        device.setStatus("DISABLED");
        when(deviceRepository.findByDeviceCode("METRO-001-ST-001-DV-001")).thenReturn(Optional.of(device));
        when(deviceRepository.save(any(Device.class))).thenReturn(device);

        SubmitHeartbeatRequest request = SubmitHeartbeatRequest.builder()
                .sentAt(LocalDateTime.now())
                .status("ACTIVE")
                .firmwareVersion("1.0.1")
                .build();

        var response = deviceIntegrationService.submitHeartbeat(
                "METRO-001-ST-001-DV-001", "valid-secret", request
        );

        assertTrue(response.isAccepted());
        verify(deviceRepository).save(device);
        assertEquals("DISABLED", device.getStatus());
    }

    @Test
    void submitHeartbeat_DeviceSecretNull_ThrowsDeviceAuthFailed() {
        device.setDeviceSecret(null);
        when(deviceRepository.findByDeviceCode("METRO-001-ST-001-DV-001")).thenReturn(Optional.of(device));

        SubmitHeartbeatRequest request = SubmitHeartbeatRequest.builder()
                .sentAt(LocalDateTime.now())
                .status("ACTIVE")
                .firmwareVersion("1.0.1")
                .build();

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.submitHeartbeat("METRO-001-ST-001-DV-001", "valid-secret", request)
        );

        assertEquals(ErrorCode.DEVICE_AUTH_FAILED, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
        verify(deviceHeartbeatRepository, never()).save(any());
    }

    @Test
    void submitIncident_NullMessage_SavesNullMessageToMongo() {
        when(deviceRepository.findByDeviceCode("METRO-001-ST-001-DV-001")).thenReturn(Optional.of(device));
        
        DeviceIncident savedMongoIncident = DeviceIncident.builder()
                .id("mongo-incident-id")
                .build();
        when(deviceIncidentRepository.save(any(DeviceIncident.class))).thenReturn(savedMongoIncident);

        SubmitIncidentRequest request = SubmitIncidentRequest.builder()
                .incidentType("GATE_JAMMED")
                .severity("HIGH")
                .occurredAt(LocalDateTime.now())
                .message(null)
                .build();

        var response = deviceIntegrationService.submitIncident(
                "METRO-001-ST-001-DV-001", "valid-secret", request
        );

        assertTrue(response.isAccepted());
        
        ArgumentCaptor<DeviceIncident> incidentCaptor = ArgumentCaptor.forClass(DeviceIncident.class);
        verify(deviceIncidentRepository).save(incidentCaptor.capture());
        DeviceIncident savedIncident = incidentCaptor.getValue();
        assertEquals(null, savedIncident.getMessage());
    }
}
