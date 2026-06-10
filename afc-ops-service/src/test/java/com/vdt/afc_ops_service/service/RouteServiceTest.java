package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.dto.request.route.CreateRouteRequest;
import com.vdt.afc_ops_service.dto.request.route.UpdateRouteRequest;
import com.vdt.afc_ops_service.dto.response.route.RouteResponse;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.mapper.RouteMapper;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import com.vdt.afc_ops_service.repository.RouteRepository;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.Impl.RouteService;
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
class RouteServiceTest {

    @Mock
    OperatorRepository operatorRepository;

    @Mock
    RouteRepository routeRepository;

    RouteMapper routeMapper = new RouteMapper();

    Validator validator;

    RouteService routeService;

    @BeforeEach
    void setUpAuthentication() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        routeService = new RouteService(
                routeRepository,
                routeMapper,
                new RouteCodeGenerator(routeRepository),
                new SecurityUtils(operatorRepository)
        );
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
    void listRoutes_ValidFilters_ReturnsMappedPage() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        Route route = Route.builder()
                .id(10L)
                .operator(operator)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .transportType(PredefinedTransportType.METRO)
                .status(PredefinedMasterDataStatus.ACTIVE)
                .build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.searchRoutes(
                1L,
                "%metro%",
                PredefinedTransportType.METRO,
                PredefinedMasterDataStatus.ACTIVE,
                PageRequest.of(0, 20)
        )).thenReturn(new PageImpl<>(List.of(route), PageRequest.of(0, 20), 1));

        var response = routeService.listRoutes(" Metro ", " metro ", " active ", 0, 20);

        assertEquals(1, response.getTotalElements());
        assertEquals("METRO-001", response.getItems().get(0).getRouteCode());
    }

    @Test
    void listRoutes_InvalidPage_ThrowsInvalidPageRequest() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> routeService.listRoutes(null, null, null, -1, 20));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
        verify(routeRepository, never()).searchRoutes(any(), any(), any(), any(), any());
    }

    @Test
    void listRoutes_KeywordTooLong_ThrowsInvalidSearchKeyword() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> routeService.listRoutes("x".repeat(51), null, null, 0, 20));

        assertEquals(ErrorCode.INVALID_SEARCH_KEYWORD, exception.getErrorCode());
    }

    @Test
    void listRoutes_InvalidTransportType_ThrowsInvalidTransportType() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> routeService.listRoutes(null, "TRAIN", null, 0, 20));

        assertEquals(ErrorCode.INVALID_TRANSPORT_TYPE, exception.getErrorCode());
    }

    @Test
    void listRoutes_InvalidStatus_ThrowsInvalidMasterDataStatus() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));

        AppException exception = assertThrows(AppException.class,
                () -> routeService.listRoutes(null, null, "DELETED", 0, 20));

        assertEquals(ErrorCode.INVALID_MASTER_DATA_STATUS, exception.getErrorCode());
    }

    @Test
    void createRouteNormalizesAndPersistsRoute() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findRouteCodesByOperatorAndPrefix(operator, PredefinedTransportType.METRO))
                .thenReturn(List.of("METRO-001", "METRO-002"));
        when(routeRepository.existsByOperatorAndRouteCode(operator, "METRO-003")).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(10L);
            return route;
        });

        RouteResponse response = routeService.createRoute(CreateRouteRequest.builder()
                .routeName(" Metro Line 1 ")
                .transportType("metro")
                .build());

        assertEquals(10L, response.getId());
        assertEquals("METRO-003", response.getRouteCode());
        assertEquals(PredefinedTransportType.METRO, response.getTransportType());
        assertEquals(PredefinedMasterDataStatus.ACTIVE, response.getStatus());
        verify(routeRepository).save(any(Route.class));
    }

    @Test
    void createRouteSkipsExistingGeneratedCode() {
        Operator operator = Operator.builder().id(1L).build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findRouteCodesByOperatorAndPrefix(operator, PredefinedTransportType.METRO))
                .thenReturn(List.of("METRO-001"));
        when(routeRepository.existsByOperatorAndRouteCode(operator, "METRO-002")).thenReturn(true);
        when(routeRepository.existsByOperatorAndRouteCode(operator, "METRO-003")).thenReturn(false);
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(10L);
            return route;
        });

        RouteResponse response = routeService.createRoute(CreateRouteRequest.builder()
                .routeName("Metro Line 1")
                .transportType("METRO")
                .build());

        assertEquals("METRO-003", response.getRouteCode());
    }

    @Test
    void createRoute_OperatorMissing_ThrowsOperatorNotFound() {
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> routeService.createRoute(CreateRouteRequest.builder()
                .routeName("Metro Line 1")
                .transportType("METRO")
                .build()));

        assertEquals(ErrorCode.OPERATOR_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void disableRoute_DifferentOperator_ThrowsOperatorAccessDenied() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.empty());
        when(routeRepository.existsById(10L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> routeService.disableRoute(10L));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void disableRoute_AlreadyDisabled_ThrowsRouteAlreadyDisabled() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        Route route = Route.builder()
                .id(10L)
                .operator(operator)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .transportType(PredefinedTransportType.METRO)
                .status(PredefinedMasterDataStatus.DISABLED)
                .build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));

        AppException exception = assertThrows(AppException.class, () -> routeService.disableRoute(10L));

        assertEquals(ErrorCode.ROUTE_ALREADY_DISABLED, exception.getErrorCode());
    }

    @Test
    void disableRoute_ActiveRoute_DisablesRoute() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        Route route = Route.builder()
                .id(10L)
                .operator(operator)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .transportType(PredefinedTransportType.METRO)
                .status(PredefinedMasterDataStatus.ACTIVE)
                .build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));
        when(routeRepository.save(route)).thenReturn(route);

        RouteResponse response = routeService.disableRoute(10L);

        assertEquals(PredefinedMasterDataStatus.DISABLED, response.getStatus());
    }

    @Test
    void updateRoute_DifferentOperator_ThrowsOperatorAccessDenied() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.empty());
        when(routeRepository.existsById(10L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> routeService.updateRoute(
                10L,
                UpdateRouteRequest.builder()
                        .routeName("Metro Line 1 Updated")
                        .transportType("METRO")
                        .build()
        ));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void updateRoute_ExistingRoute_UpdatesAllowedFields() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        Route route = Route.builder()
                .id(10L)
                .operator(operator)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .transportType(PredefinedTransportType.METRO)
                .status(PredefinedMasterDataStatus.ACTIVE)
                .build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));
        when(routeRepository.save(route)).thenReturn(route);

        RouteResponse response = routeService.updateRoute(
                10L,
                UpdateRouteRequest.builder()
                        .routeName(" Bus Route 01 ")
                        .transportType(" bus ")
                        .build()
        );

        assertEquals("Bus Route 01", response.getRouteName());
        assertEquals(PredefinedTransportType.BUS, response.getTransportType());
        assertEquals("METRO-001", response.getRouteCode());
    }

    @Test
    void updateRoute_MissingRoute_ThrowsRouteNotFound() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.empty());
        when(routeRepository.existsById(10L)).thenReturn(false);

        AppException exception = assertThrows(AppException.class, () -> routeService.updateRoute(
                10L,
                UpdateRouteRequest.builder()
                        .routeName("Metro Line 1")
                        .transportType("METRO")
                        .build()
        ));

        assertEquals(ErrorCode.ROUTE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void updateRoute_InvalidRouteId_ThrowsInvalidRouteId() {
        AppException exception = assertThrows(AppException.class, () -> routeService.updateRoute(
                0L,
                UpdateRouteRequest.builder()
                        .routeName("Metro Line 1")
                        .transportType("METRO")
                        .build()
        ));

        assertEquals(ErrorCode.INVALID_ROUTE_ID, exception.getErrorCode());
    }

    @Test
    void enableRoute_DifferentOperator_ThrowsOperatorAccessDenied() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.empty());
        when(routeRepository.existsById(10L)).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () -> routeService.enableRoute(10L));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void enableRoute_DisabledRoute_EnablesRoute() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        Route route = Route.builder()
                .id(10L)
                .operator(operator)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .transportType(PredefinedTransportType.METRO)
                .status(PredefinedMasterDataStatus.DISABLED)
                .build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));
        when(routeRepository.save(route)).thenReturn(route);

        RouteResponse response = routeService.enableRoute(10L);

        assertEquals(PredefinedMasterDataStatus.ACTIVE, response.getStatus());
    }

    @Test
    void enableRoute_AlreadyEnabled_ThrowsRouteAlreadyEnabled() {
        Operator operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        Route route = Route.builder()
                .id(10L)
                .operator(operator)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .transportType(PredefinedTransportType.METRO)
                .status(PredefinedMasterDataStatus.ACTIVE)
                .build();
        when(operatorRepository.findByOperatorCode("HCMC-METRO")).thenReturn(Optional.of(operator));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));

        AppException exception = assertThrows(AppException.class, () -> routeService.enableRoute(10L));

        assertEquals(ErrorCode.ROUTE_ALREADY_ENABLED, exception.getErrorCode());
    }

    @Test
    void createRouteRequestRejectsInvalidTransportType() {
        Set<ConstraintViolation<CreateRouteRequest>> violations = validator.validate(
                CreateRouteRequest.builder()
                        .routeName("Air Route")
                        .transportType("AIR")
                        .build()
        );

        assertTrue(violations.stream()
                .anyMatch(violation -> ErrorCode.INVALID_TRANSPORT_TYPE.name().equals(violation.getMessage())));
    }
}
