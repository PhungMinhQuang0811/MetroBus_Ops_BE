package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.dto.request.station.CreateStationRequest;
import com.vdt.afc_ops_service.dto.request.station.UpdateStationRequest;
import com.vdt.afc_ops_service.dto.response.station.StationResponse;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.mapper.StationMapper;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import com.vdt.afc_ops_service.repository.RouteRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.Impl.StationService;
import com.vdt.afc_ops_service.service.generator.StationCodeGenerator;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    OperatorRepository operatorRepository;

    @Mock
    RouteRepository routeRepository;

    @Mock
    StationRepository stationRepository;

    @Mock
    DeviceRepository deviceRepository;

    Validator validator;
    StationService stationService;
    Operator operator;
    Route route;

    @BeforeEach
    void setUpAuthentication() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        stationService = new StationService(
                stationRepository,
                deviceRepository,
                routeRepository,
                new StationMapper(),
                new StationCodeGenerator(stationRepository),
                new SecurityUtils(operatorRepository)
        );
        operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        route = Route.builder().id(10L).operator(operator).routeCode("METRO-001").build();
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
    void listStations_ValidFilters_ReturnsMappedPage() {
        Station station = station(100L, route, PredefinedMasterDataStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));
        when(stationRepository.searchStations(
                1L,
                10L,
                "%ben%",
                PredefinedMasterDataStatus.ACTIVE,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(station), PageRequest.of(0, 20), 1));

        var response = stationService.listStations(10L, " Ben ", " active ", 0, 20);

        assertEquals(1, response.getTotalElements());
        assertEquals("METRO-001-ST-001", response.getItems().get(0).getStationCode());
    }

    @Test
    void getStation_ExistingStation_ReturnsRouteAndDeviceSummary() {
        route.setRouteName("Metro Line 1");
        Station station = station(100L, route, PredefinedMasterDataStatus.ACTIVE);
        Device activeDevice = device(1L, station, "GATE-001", "ACTIVE");
        Device maintenanceDevice = device(2L, station, "GATE-002", "MAINTENANCE");
        Device offlineDevice = device(3L, station, "GATE-003", "OFFLINE");
        Device disabledDevice = device(4L, station, "GATE-004", "DISABLED");
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(deviceRepository.findAllByStationOrderByDeviceCodeAsc(station))
                .thenReturn(List.of(activeDevice, maintenanceDevice, offlineDevice, disabledDevice));

        var response = stationService.getStation(100L);

        assertEquals("Metro Line 1", response.getRouteName());
        assertEquals(4, response.getDeviceSummary().getTotal());
        assertEquals(1, response.getDeviceSummary().getActive());
        assertEquals(1, response.getDeviceSummary().getOffline());
        assertEquals(1, response.getDeviceSummary().getMaintenance());
        assertEquals(1, response.getDeviceSummary().getDisabled());
        assertEquals("GATE-001", response.getDevices().get(0).getDeviceCode());
    }

    @Test
    void getStation_DifferentOperator_ThrowsOperatorAccessDenied() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> stationService.getStation(100L));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void listStations_NoRouteFilter_SearchesWithinCurrentOperator() {
        Station station = station(100L, route, PredefinedMasterDataStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.searchStations(
                1L,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(station), PageRequest.of(0, 20), 1));

        var response = stationService.listStations(null, null, null, 0, 20);

        assertEquals(1, response.getTotalElements());
        verify(routeRepository, never()).findByIdAndOperator(any(), any());
    }

    @Test
    void listStations_RouteBelongsToOtherOperator_ThrowsOperatorAccessDenied() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.empty());
        when(routeRepository.existsById(10L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> stationService.listStations(10L, null, null, 0, 20));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void listStations_MissingRoute_ThrowsRouteNotFound() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.empty());
        when(routeRepository.existsById(10L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> stationService.listStations(10L, null, null, 0, 20));

        assertEquals(ErrorCode.ROUTE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void listStations_InvalidRouteId_ThrowsInvalidRouteId() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> stationService.listStations(0L, null, null, 0, 20));

        assertEquals(ErrorCode.INVALID_ROUTE_ID, exception.getErrorCode());
    }

    @Test
    void listStations_InvalidPage_ThrowsInvalidPageRequest() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> stationService.listStations(null, null, null, -1, 20));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void listStations_KeywordTooLong_ThrowsInvalidSearchKeyword() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> stationService.listStations(null, "x".repeat(51), null, 0, 20));

        assertEquals(ErrorCode.INVALID_SEARCH_KEYWORD, exception.getErrorCode());
    }

    @Test
    void listStations_InvalidStatus_ThrowsInvalidMasterDataStatus() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> stationService.listStations(null, null, "DELETED", 0, 20));

        assertEquals(ErrorCode.INVALID_MASTER_DATA_STATUS, exception.getErrorCode());
    }

    @Test
    void createStation_NormalizesAndPersistsStation() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));
        when(stationRepository.existsByRouteAndStationOrder(route, 1)).thenReturn(false);
        when(stationRepository.findStationCodesByRouteAndPrefix(route, "METRO-001-ST"))
                .thenReturn(List.of("METRO-001-ST-001"));
        when(stationRepository.existsByRouteAndStationCode(route, "METRO-001-ST-002")).thenReturn(false);
        when(stationRepository.save(any(Station.class))).thenAnswer(invocation -> {
            Station station = invocation.getArgument(0);
            station.setId(100L);
            return station;
        });

        StationResponse response = stationService.createStation(CreateStationRequest.builder()
                .routeId(10L)
                .stationName(" Ben Thanh ")
                .stationOrder(1)
                .build());

        assertEquals(100L, response.getId());
        assertEquals("METRO-001-ST-002", response.getStationCode());
        assertEquals("Ben Thanh", response.getStationName());
        assertEquals(PredefinedMasterDataStatus.ACTIVE, response.getStatus());
    }

    @Test
    void createStation_DuplicateOrder_ThrowsStationOrderExisted() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));
        when(stationRepository.existsByRouteAndStationOrder(route, 1)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> stationService.createStation(CreateStationRequest.builder()
                        .routeId(10L)
                        .stationName("Ben Thanh")
                        .stationOrder(1)
                        .build()));

        assertEquals(ErrorCode.STATION_ORDER_EXISTED, exception.getErrorCode());
        verify(stationRepository, never()).save(any());
    }

    @Test
    void createStation_MissingRoute_ThrowsRouteNotFound() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.empty());
        when(routeRepository.existsById(10L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> stationService.createStation(CreateStationRequest.builder()
                        .routeId(10L)
                        .stationName("Ben Thanh")
                        .stationOrder(1)
                        .build()));

        assertEquals(ErrorCode.ROUTE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateStation_ExistingStation_UpdatesAllowedFields() {
        Station station = station(100L, route, PredefinedMasterDataStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));
        when(stationRepository.existsByRouteAndStationOrderAndIdNot(route, 2, 100L)).thenReturn(false);
        when(stationRepository.save(station)).thenReturn(station);

        StationResponse response = stationService.updateStation(100L, UpdateStationRequest.builder()
                .routeId(10L)
                .stationName(" Opera House ")
                .stationOrder(2)
                .build());

        assertEquals("Opera House", response.getStationName());
        assertEquals(2, response.getStationOrder());
        assertEquals("METRO-001-ST-001", response.getStationCode());
    }

    @Test
    void updateStation_StationBelongsToOtherOperator_ThrowsOperatorAccessDenied() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> stationService.updateStation(100L, UpdateStationRequest.builder()
                        .routeId(10L)
                        .stationName("Ben Thanh")
                        .stationOrder(1)
                        .build()));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void updateStation_MissingStation_ThrowsStationNotFound() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.empty());
        when(stationRepository.existsById(100L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> stationService.updateStation(100L, UpdateStationRequest.builder()
                        .routeId(10L)
                        .stationName("Ben Thanh")
                        .stationOrder(1)
                        .build()));

        assertEquals(ErrorCode.STATION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateStation_RouteBelongsToOtherOperator_ThrowsOperatorAccessDenied() {
        Station station = station(100L, route, PredefinedMasterDataStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(routeRepository.findByIdAndOperator(20L, operator)).thenReturn(Optional.empty());
        when(routeRepository.existsById(20L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> stationService.updateStation(100L, UpdateStationRequest.builder()
                        .routeId(20L)
                        .stationName("Ben Thanh")
                        .stationOrder(1)
                        .build()));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void updateStation_DuplicateOrder_ThrowsStationOrderExisted() {
        Station station = station(100L, route, PredefinedMasterDataStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));
        when(stationRepository.existsByRouteAndStationOrderAndIdNot(route, 2, 100L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class,
                () -> stationService.updateStation(100L, UpdateStationRequest.builder()
                        .routeId(10L)
                        .stationName("Opera House")
                        .stationOrder(2)
                        .build()));

        assertEquals(ErrorCode.STATION_ORDER_EXISTED, exception.getErrorCode());
        verify(stationRepository, never()).save(any());
    }

    @Test
    void enableStation_AlreadyEnabled_ThrowsStationAlreadyEnabled() {
        Station station = station(100L, route, PredefinedMasterDataStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));

        AppException exception = assertThrows(AppException.class, () -> stationService.enableStation(100L));

        assertEquals(ErrorCode.STATION_ALREADY_ENABLED, exception.getErrorCode());
    }

    @Test
    void disableStation_AlreadyDisabled_ThrowsStationAlreadyDisabled() {
        Station station = station(100L, route, PredefinedMasterDataStatus.DISABLED);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));

        AppException exception = assertThrows(AppException.class, () -> stationService.disableStation(100L));

        assertEquals(ErrorCode.STATION_ALREADY_DISABLED, exception.getErrorCode());
    }

    @Test
    void enableStation_DisabledStation_EnablesStation() {
        Station station = station(100L, route, PredefinedMasterDataStatus.DISABLED);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(station)).thenReturn(station);

        StationResponse response = stationService.enableStation(100L);

        assertEquals(PredefinedMasterDataStatus.ACTIVE, response.getStatus());
    }

    @Test
    void disableStation_ActiveStation_DisablesStation() {
        Station station = station(100L, route, PredefinedMasterDataStatus.ACTIVE);
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(stationRepository.save(station)).thenReturn(station);

        StationResponse response = stationService.disableStation(100L);

        assertEquals(PredefinedMasterDataStatus.DISABLED, response.getStatus());
    }

    @Test
    void enableStation_InvalidStationId_ThrowsInvalidStationId() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class, () -> stationService.enableStation(0L));

        assertEquals(ErrorCode.INVALID_STATION_ID, exception.getErrorCode());
    }

    @Test
    void createStationRequestRejectsInvalidOrder() {
        Set<ConstraintViolation<CreateStationRequest>> violations = validator.validate(
                CreateStationRequest.builder()
                        .routeId(10L)
                        .stationName("Ben Thanh")
                        .stationOrder(0)
                        .build()
        );

        assertTrue(violations.stream()
                .anyMatch(violation -> ErrorCode.INVALID_STATION_ORDER.name().equals(violation.getMessage())));
    }

    private Station station(Long id, Route route, String status) {
        return Station.builder()
                .id(id)
                .route(route)
                .stationCode("METRO-001-ST-001")
                .stationName("Ben Thanh")
                .stationOrder(1)
                .status(status)
                .createdByAccountId("account-1")
                .build();
    }

    private Device device(Long id, Station station, String deviceCode, String status) {
        return Device.builder()
                .id(id)
                .station(station)
                .deviceCode(deviceCode)
                .deviceType("QR_SCANNER_SIMULATOR")
                .direction("ENTRY")
                .status(status)
                .build();
    }
}
