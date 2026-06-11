package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedDeviceStatus;
import com.vdt.afc_ops_service.constant.PredefinedDeviceType;
import com.vdt.afc_ops_service.dto.request.device.CreateDeviceRequest;
import com.vdt.afc_ops_service.dto.request.device.UpdateDeviceRequest;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.mapper.DeviceMapper;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.generator.DeviceCodeGenerator;
import com.vdt.afc_ops_service.service.Impl.DeviceService;
import com.vdt.afc_ops_service.service.generator.DeviceSecretGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    OperatorRepository operatorRepository;

    @Mock
    StationRepository stationRepository;

    @Mock
    DeviceRepository deviceRepository;

    @Mock
    DeviceCodeGenerator deviceCodeGenerator;

    @Mock
    DeviceSecretGenerator deviceSecretGenerator;

    DeviceService deviceService;
    Operator operator;
    Route route;
    Station station;

    @BeforeEach
    void setUpAuthentication() {
        deviceService = new DeviceService(
                deviceRepository,
                stationRepository,
                new DeviceMapper(),
                deviceCodeGenerator,
                deviceSecretGenerator,
                new SecurityUtils(operatorRepository)
        );
        operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        route = Route.builder().id(10L).operator(operator).routeCode("METRO-001").build();
        station = Station.builder()
                .id(100L)
                .route(route)
                .stationCode("METRO-001-ST-001")
                .stationName("Ben Thanh")
                .build();
        AfcUserDetails principal = AfcUserDetails.builder()
                .id("account-1")
                .username("manager")
                .operatorCode("HCMC-METRO")
                .authorities(List.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listDevices_ValidFilters_ReturnsMappedPage() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(deviceRepository.searchDevices(
                1L,
                100L,
                PredefinedDeviceType.QR_SCANNER_SIMULATOR,
                PredefinedDeviceStatus.ACTIVE,
                "%gate%",
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(device), PageRequest.of(0, 20), 1));

        var response = deviceService.listDevices(100L, " qr_scanner_simulator ", " active ", " gate ", 0, 20);

        assertEquals(1, response.getTotalElements());
        assertEquals("GATE-001", response.getItems().get(0).getDeviceCode());
        assertEquals("Ben Thanh", response.getItems().get(0).getStationName());
    }

    @Test
    void listDevices_NoStationFilter_SearchesWithinCurrentOperator() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.searchDevices(
                1L,
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(device), PageRequest.of(0, 20), 1));

        var response = deviceService.listDevices(null, null, null, null, 0, 20);

        assertEquals(1, response.getTotalElements());
        verify(stationRepository, never()).findByIdAndRouteOperatorId(any(), any());
    }

    @Test
    void listDevices_InvalidStationId_ThrowsInvalidStationId() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.listDevices(0L, null, null, null, 0, 20));

        assertEquals(ErrorCode.INVALID_STATION_ID, exception.getErrorCode());
    }

    @Test
    void listDevices_InvalidPage_ThrowsInvalidPageRequest() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.listDevices(null, null, null, null, -1, 20));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void listDevices_SizeZero_ThrowsInvalidPageRequest() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.listDevices(null, null, null, null, 0, 0));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void listDevices_SizeTooLarge_ThrowsInvalidPageRequest() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.listDevices(null, null, null, null, 0, 101));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void listDevices_KeywordTooLong_ThrowsInvalidSearchKeyword() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.listDevices(null, null, null, "x".repeat(51), 0, 20));

        assertEquals(ErrorCode.INVALID_SEARCH_KEYWORD, exception.getErrorCode());
    }

    @Test
    void listDevices_InvalidDeviceType_ThrowsInvalidDeviceType() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.listDevices(null, "GATE", null, null, 0, 20));

        assertEquals(ErrorCode.INVALID_DEVICE_TYPE, exception.getErrorCode());
    }

    @Test
    void listDevices_InvalidStatus_ThrowsInvalidDeviceStatus() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.listDevices(null, null, "DELETED", null, 0, 20));

        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
    }

    @Test
    void listDevices_OfflineStatus_SearchesWithinCurrentOperator() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.searchDevices(
                1L,
                null,
                null,
                PredefinedDeviceStatus.OFFLINE,
                null,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = deviceService.listDevices(null, null, " offline ", null, 0, 20);

        assertEquals(0, response.getTotalElements());
    }

    @Test
    void listDevices_MaintenanceStatus_SearchesWithinCurrentOperator() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.searchDevices(
                1L,
                null,
                null,
                PredefinedDeviceStatus.MAINTENANCE,
                null,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = deviceService.listDevices(null, null, " maintenance ", null, 0, 20);

        assertEquals(0, response.getTotalElements());
    }

    @Test
    void listDevices_DisabledStatus_SearchesWithinCurrentOperator() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.searchDevices(
                1L,
                null,
                null,
                PredefinedDeviceStatus.DISABLED,
                null,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = deviceService.listDevices(null, null, " disabled ", null, 0, 20);

        assertEquals(0, response.getTotalElements());
    }

    @Test
    void listDevices_StationBelongsToOtherOperator_ThrowsOperatorAccessDenied() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.listDevices(100L, null, null, null, 0, 20));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void listDevices_MissingStation_ThrowsStationNotFound() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.listDevices(100L, null, null, null, 0, 20));

        assertEquals(ErrorCode.STATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getDevice_ExistingDevice_ReturnsStationRouteAndOperationFields() {
        route.setRouteName("Metro Line 1");
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));

        var response = deviceService.getDevice(200L);

        assertEquals("GATE-001", response.getDeviceCode());
        assertEquals("METRO-001-ST-001", response.getStationCode());
        assertEquals("Ben Thanh", response.getStationName());
        assertEquals("METRO-001", response.getRouteCode());
        assertEquals("Metro Line 1", response.getRouteName());
        assertEquals("1.0.0", response.getFirmwareVersion());
        assertEquals(null, response.getLatestIncident());
    }

    @Test
    void getDevice_DifferentOperator_ThrowsOperatorAccessDenied() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.empty());
        when(deviceRepository.existsById(200L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> deviceService.getDevice(200L));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void getDevice_MissingDevice_ThrowsDeviceNotFound() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.empty());
        when(deviceRepository.existsById(200L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> deviceService.getDevice(200L));

        assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getDevice_NullDeviceId_ThrowsInvalidDeviceId() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class, () -> deviceService.getDevice(null));

        assertEquals(ErrorCode.INVALID_DEVICE_ID, exception.getErrorCode());
    }

    @Test
    void getDevice_InvalidDeviceId_ThrowsInvalidDeviceId() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class, () -> deviceService.getDevice(0L));

        assertEquals(ErrorCode.INVALID_DEVICE_ID, exception.getErrorCode());
    }

    @Test
    void createDevice_NormalizesAndPersistsDevice() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(deviceCodeGenerator.generate(station)).thenReturn("METRO-001-ST-001-DV-001");
        when(deviceSecretGenerator.generate()).thenReturn("generated-secret");
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            device.setId(200L);
            return device;
        });

        var response = deviceService.createDevice(CreateDeviceRequest.builder()
                .stationId(100L)
                .deviceType(" qr_scanner_simulator ")
                .direction(" entry ")
                .firmwareVersion(" 1.0.0 ")
                .build());

        assertEquals(200L, response.getId());
        assertEquals("METRO-001-ST-001-DV-001", response.getDeviceCode());
        assertEquals(PredefinedDeviceType.QR_SCANNER_SIMULATOR, response.getDeviceType());
        assertEquals(PredefinedDeviceStatus.ACTIVE, response.getStatus());
        assertEquals("1.0.0", response.getFirmwareVersion());
        assertEquals("generated-secret", response.getDeviceSecret());
    }

    @Test
    void createDevice_MissingStation_ThrowsStationNotFound() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.createDevice(CreateDeviceRequest.builder()
                        .stationId(100L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .build()));

        assertEquals(ErrorCode.STATION_NOT_FOUND, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void createDevice_NullStationId_ThrowsInvalidStationId() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.createDevice(CreateDeviceRequest.builder()
                        .stationId(null)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .build()));

        assertEquals(ErrorCode.INVALID_STATION_ID, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void createDevice_InvalidStationId_ThrowsInvalidStationId() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.createDevice(CreateDeviceRequest.builder()
                        .stationId(0L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .build()));

        assertEquals(ErrorCode.INVALID_STATION_ID, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void createDevice_StationBelongsToOtherOperator_ThrowsOperatorAccessDenied() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.createDevice(CreateDeviceRequest.builder()
                        .stationId(100L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .build()));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void updateDevice_ExistingDevice_UpdatesAllowedFields() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(deviceRepository.save(device)).thenReturn(device);

        var response = deviceService.updateDevice(200L, UpdateDeviceRequest.builder()
                .stationId(100L)
                .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                .direction("BOTH")
                .status(PredefinedDeviceStatus.MAINTENANCE)
                .firmwareVersion("1.0.1")
                .build());

        assertEquals("BOTH", response.getDirection());
        assertEquals(PredefinedDeviceStatus.MAINTENANCE, response.getStatus());
        assertEquals("1.0.1", response.getFirmwareVersion());
    }

    @Test
    void updateDevice_DisabledStatus_UpdatesAllowedFields() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(deviceRepository.save(device)).thenReturn(device);

        var response = deviceService.updateDevice(200L, UpdateDeviceRequest.builder()
                .stationId(100L)
                .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                .direction("EXIT")
                .status(" disabled ")
                .firmwareVersion(" 1.0.2 ")
                .build());

        assertEquals("EXIT", response.getDirection());
        assertEquals(PredefinedDeviceStatus.DISABLED, response.getStatus());
        assertEquals("1.0.2", response.getFirmwareVersion());
    }

    @Test
    void updateDevice_InvalidDeviceId_ThrowsInvalidDeviceId() {
        AppException exception = assertThrows(AppException.class,
                () -> deviceService.updateDevice(0L, UpdateDeviceRequest.builder()
                        .stationId(100L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .status(PredefinedDeviceStatus.ACTIVE)
                        .build()));

        assertEquals(ErrorCode.INVALID_DEVICE_ID, exception.getErrorCode());
    }

    @Test
    void updateDevice_DifferentOperator_ThrowsOperatorAccessDenied() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.empty());
        when(deviceRepository.existsById(200L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.updateDevice(200L, UpdateDeviceRequest.builder()
                        .stationId(100L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .status(PredefinedDeviceStatus.ACTIVE)
                        .build()));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void updateDevice_MissingDevice_ThrowsDeviceNotFound() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.empty());
        when(deviceRepository.existsById(200L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.updateDevice(200L, UpdateDeviceRequest.builder()
                        .stationId(100L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .status(PredefinedDeviceStatus.ACTIVE)
                        .build()));

        assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateDevice_InvalidStatus_ThrowsInvalidDeviceStatus() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.updateDevice(200L, UpdateDeviceRequest.builder()
                        .stationId(100L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .status(PredefinedDeviceStatus.OFFLINE)
                        .build()));

        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void updateDevice_NullStatus_ThrowsInvalidDeviceStatus() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.updateDevice(200L, UpdateDeviceRequest.builder()
                        .stationId(100L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .status(null)
                        .build()));

        assertEquals(ErrorCode.INVALID_DEVICE_STATUS, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void updateDevice_StationBelongsToOtherOperator_ThrowsOperatorAccessDenied() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.updateDevice(200L, UpdateDeviceRequest.builder()
                        .stationId(100L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .status(PredefinedDeviceStatus.ACTIVE)
                        .build()));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void updateDevice_MissingStation_ThrowsStationNotFound() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> deviceService.updateDevice(200L, UpdateDeviceRequest.builder()
                        .stationId(100L)
                        .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .direction("ENTRY")
                        .status(PredefinedDeviceStatus.ACTIVE)
                        .build()));

        assertEquals(ErrorCode.STATION_NOT_FOUND, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void enableDevice_AlreadyEnabled_ThrowsDeviceAlreadyEnabled() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));

        AppException exception = assertThrows(AppException.class, () -> deviceService.enableDevice(200L));

        assertEquals(ErrorCode.DEVICE_ALREADY_ENABLED, exception.getErrorCode());
    }

    @Test
    void enableDevice_DisabledDevice_EnablesDevice() {
        Device device = device(200L, PredefinedDeviceStatus.DISABLED);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        var response = deviceService.enableDevice(200L);

        assertEquals(PredefinedDeviceStatus.ACTIVE, response.getStatus());
    }

    @Test
    void disableDevice_AlreadyDisabled_ThrowsDeviceAlreadyDisabled() {
        Device device = device(200L, PredefinedDeviceStatus.DISABLED);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));

        AppException exception = assertThrows(AppException.class, () -> deviceService.disableDevice(200L));

        assertEquals(ErrorCode.DEVICE_ALREADY_DISABLED, exception.getErrorCode());
    }

    @Test
    void disableDevice_ActiveDevice_DisablesDevice() {
        Device device = device(200L, PredefinedDeviceStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(deviceRepository.findByIdAndStationRouteOperatorId(200L, 1L)).thenReturn(Optional.of(device));
        when(deviceRepository.save(device)).thenReturn(device);

        var response = deviceService.disableDevice(200L);

        assertEquals(PredefinedDeviceStatus.DISABLED, response.getStatus());
    }

    private Device device(Long id, String status) {
        return Device.builder()
                .id(id)
                .station(station)
                .deviceCode("GATE-001")
                .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                .direction("ENTRY")
                .status(status)
                .firmwareVersion("1.0.0")
                .createdByAccountId("account-1")
                .build();
    }
}
