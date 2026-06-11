package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.dto.request.route.CreateRouteRequest;
import com.vdt.afc_ops_service.dto.request.route.UpdateRouteRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.route.RouteDetailResponse;
import com.vdt.afc_ops_service.dto.response.route.RouteResponse;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.mapper.RouteMapper;
import com.vdt.afc_ops_service.repository.RouteRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.IRouteService;
import com.vdt.afc_ops_service.service.generator.RouteCodeGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RouteService implements IRouteService {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_KEYWORD_LENGTH = 50;
    RouteRepository routeRepository;
    StationRepository stationRepository;
    RouteMapper routeMapper;
    RouteCodeGenerator routeCodeGenerator;
    SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RouteResponse> listRoutes(String keyword, String transportType, String status,
                                                  int page, int size) {
        Operator operator = securityUtils.getRequiredCurrentOperator();

        String normalizedKeyword = SearchFilterUtil.normalize(keyword);
        String normalizedTransportType = SearchFilterUtil.normalizeUppercase(transportType);
        String normalizedStatus = SearchFilterUtil.normalizeUppercase(status);
        validateListParams(normalizedKeyword, normalizedTransportType, normalizedStatus, page, size);

        Page<Route> routes = routeRepository.searchRoutes(
                operator.getId(),
                SearchFilterUtil.toKeywordPattern(normalizedKeyword),
                normalizedTransportType,
                normalizedStatus,
                PageRequest.of(page, size)
        );

        return PageResponse.<RouteResponse>builder()
                .items(routes.getContent().stream().map(routeMapper::toRouteResponse).toList())
                .page(routes.getNumber())
                .size(routes.getSize())
                .totalElements(routes.getTotalElements())
                .totalPages(routes.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RouteDetailResponse getRoute(Long routeId) {
        Route route = getRoute(routeId, securityUtils.getRequiredCurrentOperator());
        var stations = stationRepository.findAllByRouteOrderByStationOrderAsc(route);
        return routeMapper.toRouteDetailResponse(route, stations);
    }

    @Override
    @Transactional
    public RouteResponse createRoute(CreateRouteRequest request) {
        String routeName = SearchFilterUtil.normalize(request.getRouteName());
        String transportType = SearchFilterUtil.normalizeUppercase(request.getTransportType());

        Operator operator = securityUtils.getRequiredCurrentOperator();
        String routeCode = routeCodeGenerator.generate(operator, transportType);

        String accountId = SecurityUtils.getRequiredCurrentAccountId();
        Route route = Route.builder()
                .operator(operator)
                .routeCode(routeCode)
                .routeName(routeName)
                .transportType(transportType)
                .status(PredefinedMasterDataStatus.ACTIVE)
                .createdByAccountId(accountId)
                .build();

        return routeMapper.toRouteResponse(routeRepository.save(route));
    }

    @Override
    @Transactional
    public RouteResponse updateRoute(Long routeId, UpdateRouteRequest request) {
        validateRouteId(routeId);
        Operator operator = securityUtils.getRequiredCurrentOperator();
        Route route = getRoute(routeId, operator);
        String routeName = SearchFilterUtil.normalize(request.getRouteName());
        String transportType = SearchFilterUtil.normalizeUppercase(request.getTransportType());

        route.setRouteName(routeName);
        route.setTransportType(transportType);
        return routeMapper.toRouteResponse(routeRepository.save(route));
    }

    @Override
    @Transactional
    public RouteResponse enableRoute(Long routeId) {
        Route route = getRoute(routeId, securityUtils.getRequiredCurrentOperator());
        if (PredefinedMasterDataStatus.ACTIVE.equals(route.getStatus())) {
            throw new AppException(ErrorCode.ROUTE_ALREADY_ENABLED);
        }
        route.setStatus(PredefinedMasterDataStatus.ACTIVE);
        return routeMapper.toRouteResponse(routeRepository.save(route));
    }

    @Override
    @Transactional
    public RouteResponse disableRoute(Long routeId) {
        Route route = getRoute(routeId, securityUtils.getRequiredCurrentOperator());
        if (PredefinedMasterDataStatus.DISABLED.equals(route.getStatus())) {
            throw new AppException(ErrorCode.ROUTE_ALREADY_DISABLED);
        }
        route.setStatus(PredefinedMasterDataStatus.DISABLED);
        return routeMapper.toRouteResponse(routeRepository.save(route));
    }

    private Route getRoute(Long routeId, Operator operator) {
        validateRouteId(routeId);
        return routeRepository.findByIdAndOperator(routeId, operator)
                .orElseGet(() -> {
                    if (routeRepository.existsById(routeId)) {
                        throw new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
                    }
                    throw new AppException(ErrorCode.ROUTE_NOT_FOUND);
                });
    }

    private void validateTransportType(String transportType) {
        if (transportType != null
                && !PredefinedTransportType.METRO.equals(transportType)
                && !PredefinedTransportType.BUS.equals(transportType)) {
            throw new AppException(ErrorCode.INVALID_TRANSPORT_TYPE);
        }
    }

    private void validateStatus(String status) {
        if (status != null
                && !PredefinedMasterDataStatus.ACTIVE.equals(status)
                && !PredefinedMasterDataStatus.DISABLED.equals(status)) {
            throw new AppException(ErrorCode.INVALID_MASTER_DATA_STATUS);
        }
    }

    private void validateListParams(String keyword, String transportType, String status, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new AppException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }
        validateTransportType(transportType);
        validateStatus(status);
    }

    private void validateRouteId(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw new AppException(ErrorCode.INVALID_ROUTE_ID);
        }
    }

}
