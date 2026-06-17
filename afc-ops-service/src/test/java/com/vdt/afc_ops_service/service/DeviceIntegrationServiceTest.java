package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.document.DeviceHeartbeat;
import com.vdt.afc_ops_service.document.DeviceIncident;
import com.vdt.afc_ops_service.dto.request.device.SubmitHeartbeatRequest;
import com.vdt.afc_ops_service.dto.request.device.SubmitIncidentRequest;
import com.vdt.afc_ops_service.dto.response.device.DeviceHeartbeatHistoryResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceStatusResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceIncidentDetailResponse;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.mongo.DeviceHeartbeatRepository;
import com.vdt.afc_ops_service.repository.mongo.DeviceIncidentRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.Impl.DeviceIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    @Mock
    StationRepository stationRepository;

    @Mock
    SecurityUtils securityUtils;

    @Mock
    MongoTemplate mongoTemplate;

    DeviceIntegrationService deviceIntegrationService;
    Device device;
    Station station;

    @BeforeEach
    void setUp() {
        deviceIntegrationService = new DeviceIntegrationService(
                deviceRepository,
                deviceHeartbeatRepository,
                deviceIncidentRepository,
                stationRepository,
                securityUtils,
                mongoTemplate
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
        assertEquals(1L, savedHeartbeat.getOperatorId());
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
        assertEquals(1L, savedIncident.getOperatorId());
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

    @Test
    void getDeviceStatus_FiltersAndPaginates() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));

        LocalDateTime now = LocalDateTime.now();
        device.setLastSeenAt(now.minusSeconds(10));
        Page<Device> pageResult = new PageImpl<>(List.of(device));
        when(deviceRepository.searchDeviceStatus(eq(1L), eq(10L), eq(100L), eq("ACTIVE"), any(PageRequest.class)))
                .thenReturn(pageResult);

        var response = deviceIntegrationService.getDeviceStatus(10L, 100L, "ACTIVE", 0, 20);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        DeviceStatusResponse item = response.getItems().get(0);
        assertEquals(200L, item.getDeviceId());
        assertEquals("METRO-001-ST-001-DV-001", item.getDeviceCode());
        assertEquals(100L, item.getStationId());
        assertEquals("ACTIVE", item.getStatus());
        assertEquals(10L, item.getOfflineSeconds());
    }

    @Test
    void getDeviceStatus_InvalidStatus_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceStatus(10L, null, "INVALID", 0, 20)
        );
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
    }

    @Test
    void getDeviceStatus_OperatorAccessDeniedOnStation_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceStatus(10L, 100L, "ACTIVE", 0, 20)
        );
        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void getDeviceHeartbeats_ValidDevice_ReturnsHeartbeats() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));

        DeviceHeartbeat hb = DeviceHeartbeat.builder()
                .id("mongo-id")
                .deviceId(200L)
                .deviceCode("METRO-001-ST-001-DV-001")
                .stationId(100L)
                .status("ACTIVE")
                .firmwareVersion("1.0.1")
                .sentAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .payload(Map.of("cpu", 0.15))
                .build();

        Page<DeviceHeartbeat> pageResult = new PageImpl<>(List.of(hb));
        when(deviceHeartbeatRepository.findAllByDeviceId(eq(200L), any(PageRequest.class)))
                .thenReturn(pageResult);

        var response = deviceIntegrationService.getDeviceHeartbeats(200L, 0, 20);

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        DeviceHeartbeatHistoryResponse item = response.getItems().get(0);
        assertEquals("mongo-id", item.getId());
        assertEquals(200L, item.getDeviceId());
        assertEquals("ACTIVE", item.getStatus());
        assertEquals("1.0.1", item.getFirmwareVersion());
    }

    @Test
    void getDeviceHeartbeats_DeviceNotFound_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.empty());
        when(deviceRepository.existsById(200L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceHeartbeats(200L, 0, 20)
        );
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void searchIncidents_FiltersAndPaginates() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        DeviceIncident incident = DeviceIncident.builder()
                .id("mongo-incident-id")
                .deviceId(200L)
                .deviceCode("METRO-001-ST-001-DV-001")
                .stationId(100L)
                .incidentType("GATE_JAMMED")
                .severity("HIGH")
                .message("Gate arm jammed")
                .occurredAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .resolvedAt(null)
                .build();

        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(DeviceIncident.class)))
                .thenReturn(List.of(incident));
        when(mongoTemplate.count(any(org.springframework.data.mongodb.core.query.Query.class), eq(DeviceIncident.class)))
                .thenReturn(1L);

        var response = deviceIntegrationService.searchIncidents(
                LocalDateTime.now().minusDays(1), LocalDateTime.now(), 100L, 200L, "HIGH", "GATE_JAMMED", false, 0, 20
        );

        assertNotNull(response);
        assertEquals(1, response.getItems().size());
        DeviceIncidentResponse item = response.getItems().get(0);
        assertEquals("mongo-incident-id", item.getId());
        assertEquals("GATE_JAMMED", item.getIncidentType());
        assertEquals("HIGH", item.getSeverity());
    }

    @Test
    void getIncidentDetail_ValidId_ReturnsDetail() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        DeviceIncident incident = DeviceIncident.builder()
                .id("mongo-incident-id")
                .operatorId(1L)
                .deviceId(200L)
                .deviceCode("METRO-001-ST-001-DV-001")
                .stationId(100L)
                .incidentType("GATE_JAMMED")
                .severity("HIGH")
                .message("Gate arm jammed")
                .occurredAt(LocalDateTime.now())
                .receivedAt(LocalDateTime.now())
                .resolvedAt(null)
                .build();

        when(deviceIncidentRepository.findById("mongo-incident-id")).thenReturn(Optional.of(incident));
        when(deviceRepository.findById(200L)).thenReturn(Optional.of(device));

        var response = deviceIntegrationService.getIncidentDetail("mongo-incident-id");

        assertNotNull(response);
        assertEquals("mongo-incident-id", response.getId());
        assertEquals("GATE_JAMMED", response.getIncidentType());
        assertEquals("HIGH", response.getSeverity());
        assertEquals("QR_SCANNER_SIMULATOR", response.getDeviceType());
        assertEquals("ACTIVE", response.getDeviceStatus());
        assertEquals("Ben Thanh", response.getStationName());
    }

    @Test
    void getIncidentDetail_IncidentNotFound_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(deviceIncidentRepository.findById("missing-id")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.getIncidentDetail("missing-id")
        );
        assertEquals(ErrorCode.INCIDENT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getIncidentDetail_OperatorMismatch_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        DeviceIncident incident = DeviceIncident.builder()
                .id("mongo-incident-id")
                .operatorId(2L) // Operator mismatch!
                .build();
        when(deviceIncidentRepository.findById("mongo-incident-id")).thenReturn(Optional.of(incident));

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.getIncidentDetail("mongo-incident-id")
        );
        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void getDeviceStatus_PageRequestInvalid_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        // page < 0
        AppException ex1 = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceStatus(10L, null, "ACTIVE", -1, 20)
        );
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, ex1.getErrorCode());

        // size < 1
        AppException ex2 = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceStatus(10L, null, "ACTIVE", 0, 0)
        );
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, ex2.getErrorCode());

        // size > 100
        AppException ex3 = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceStatus(10L, null, "ACTIVE", 0, 101)
        );
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, ex3.getErrorCode());
    }

    @Test
    void getDeviceStatus_StationNotFound_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceStatus(10L, 100L, "ACTIVE", 0, 20)
        );
        assertEquals(ErrorCode.STATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getDeviceStatus_StatusNullOrWhitespace() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        Page<Device> pageResult = new PageImpl<>(List.of(device));
        when(deviceRepository.searchDeviceStatus(eq(1L), eq(10L), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(pageResult);

        // Status null is allowed
        var response = deviceIntegrationService.getDeviceStatus(10L, null, null, 0, 20);
        assertNotNull(response);

        // Status whitespace should throw INVALID_DEVICE_STATUS
        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceStatus(10L, null, "   ", 0, 20)
        );
        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
    }

    @Test
    void getDeviceStatus_LastSeenAtInFutureOrNull() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        // Device with lastSeenAt in future
        Device futureDevice = Device.builder()
                .id(201L)
                .station(station)
                .deviceCode("FUTURE-DEV")
                .status("ACTIVE")
                .lastSeenAt(LocalDateTime.now().plusHours(1))
                .build();

        // Device with lastSeenAt null
        Device nullDevice = Device.builder()
                .id(202L)
                .station(station)
                .deviceCode("NULL-DEV")
                .status("ACTIVE")
                .lastSeenAt(null)
                .build();

        Page<Device> pageResult = new PageImpl<>(List.of(futureDevice, nullDevice));
        when(deviceRepository.searchDeviceStatus(eq(1L), eq(null), eq(null), eq(null), any(PageRequest.class)))
                .thenReturn(pageResult);

        var response = deviceIntegrationService.getDeviceStatus(null, null, null, 0, 20);
        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        
        // Future lastSeenAt -> offlineSeconds is 0
        assertEquals(0L, response.getItems().get(0).getOfflineSeconds());
        
        // Null lastSeenAt -> offlineSeconds is null
        assertEquals(null, response.getItems().get(1).getOfflineSeconds());
    }

    @Test
    void getDeviceHeartbeats_PageRequestInvalid_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        AppException ex = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceHeartbeats(200L, -1, 20)
        );
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, ex.getErrorCode());
    }

    @Test
    void getDeviceHeartbeats_OperatorAccessDenied_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.empty());
        when(deviceRepository.existsById(200L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.getDeviceHeartbeats(200L, 0, 20)
        );
        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void searchIncidents_PageRequestInvalid_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        AppException ex = assertThrows(AppException.class, () ->
                deviceIntegrationService.searchIncidents(null, null, null, null, null, null, null, 0, 0)
        );
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, ex.getErrorCode());
    }

    @Test
    void searchIncidents_QueryCriteriaBranches() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ArgumentCaptor<org.springframework.data.mongodb.core.query.Query> queryCaptor =
                ArgumentCaptor.forClass(org.springframework.data.mongodb.core.query.Query.class);

        when(mongoTemplate.find(queryCaptor.capture(), eq(DeviceIncident.class))).thenReturn(List.of());
        when(mongoTemplate.count(any(org.springframework.data.mongodb.core.query.Query.class), eq(DeviceIncident.class))).thenReturn(0L);

        LocalDateTime now = LocalDateTime.now();

        // 1. Check from != null, to == null, resolved = true, empty severity/incidentType
        deviceIntegrationService.searchIncidents(now, null, null, null, "   ", "", true, 0, 20);
        org.springframework.data.mongodb.core.query.Query query1 = queryCaptor.getValue();
        assertTrue(query1.getQueryObject().containsKey("occurredAt"));
        assertTrue(query1.getQueryObject().containsKey("resolvedAt"));
        assertTrue(!query1.getQueryObject().containsKey("severity"));
        assertTrue(!query1.getQueryObject().containsKey("incidentType"));

        // 2. Check from == null, to != null, resolved = false, null severity/incidentType
        deviceIntegrationService.searchIncidents(null, now, null, null, null, null, false, 0, 20);
        org.springframework.data.mongodb.core.query.Query query2 = queryCaptor.getValue();
        assertTrue(query2.getQueryObject().containsKey("occurredAt"));
        assertTrue(query2.getQueryObject().containsKey("resolvedAt"));
        assertTrue(!query2.getQueryObject().containsKey("severity"));
        assertTrue(!query2.getQueryObject().containsKey("incidentType"));

        // 3. Check resolved == null, and check that occurredAt is not in query when both from and to are null
        deviceIntegrationService.searchIncidents(null, null, null, null, null, null, null, 0, 20);
        org.springframework.data.mongodb.core.query.Query query3 = queryCaptor.getValue();
        assertTrue(!query3.getQueryObject().containsKey("occurredAt"));
        assertTrue(!query3.getQueryObject().containsKey("resolvedAt"));
    }

    @Test
    void getIncidentDetail_DeviceNotFound_ThrowsException() {
        Operator operator = Operator.builder().id(1L).build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        DeviceIncident incident = DeviceIncident.builder()
                .id("mongo-incident-id")
                .operatorId(1L)
                .deviceId(200L)
                .build();

        when(deviceIncidentRepository.findById("mongo-incident-id")).thenReturn(Optional.of(incident));
        when(deviceRepository.findById(200L)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () ->
                deviceIntegrationService.getIncidentDetail("mongo-incident-id")
        );
        assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
    }
}
