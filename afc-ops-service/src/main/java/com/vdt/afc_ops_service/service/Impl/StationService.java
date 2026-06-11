package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.dto.request.station.CreateStationRequest;
import com.vdt.afc_ops_service.dto.request.station.UpdateStationRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.station.StationDetailResponse;
import com.vdt.afc_ops_service.dto.response.station.StationResponse;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.mapper.StationMapper;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.RouteRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.IStationService;
import com.vdt.afc_ops_service.service.generator.StationCodeGenerator;
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
public class StationService implements IStationService {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_KEYWORD_LENGTH = 50;

    StationRepository stationRepository;
    DeviceRepository deviceRepository;
    RouteRepository routeRepository;
    StationMapper stationMapper;
    StationCodeGenerator stationCodeGenerator;
    SecurityUtils securityUtils;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StationResponse> listStations(Long routeId, String keyword, String status, int page, int size) {
        Operator operator = securityUtils.getRequiredCurrentOperator();

        String normalizedKeyword = SearchFilterUtil.normalize(keyword);
        String normalizedStatus = SearchFilterUtil.normalizeUppercase(status);
        validateListParams(routeId, normalizedKeyword, normalizedStatus, page, size);
        if (routeId != null) {
            getRoute(routeId, operator);
        }

        Page<Station> stations = stationRepository.searchStations(
                operator.getId(),
                routeId,
                SearchFilterUtil.toKeywordPattern(normalizedKeyword),
                normalizedStatus,
                PageRequest.of(page, size)
        );

        return PageResponse.<StationResponse>builder()
                .items(stations.getContent().stream().map(stationMapper::toStationResponse).toList())
                .page(stations.getNumber())
                .size(stations.getSize())
                .totalElements(stations.getTotalElements())
                .totalPages(stations.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public StationDetailResponse getStation(Long stationId) {
        Station station = getStation(stationId, securityUtils.getRequiredCurrentOperator());
        var devices = deviceRepository.findAllByStationOrderByDeviceCodeAsc(station);
        return stationMapper.toStationDetailResponse(station, devices);
    }

    @Override
    @Transactional
    public StationResponse createStation(CreateStationRequest request) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        Route route = getRoute(request.getRouteId(), operator);
        validateStationOrderAvailable(route, request.getStationOrder(), null);

        Station station = Station.builder()
                .route(route)
                .stationCode(stationCodeGenerator.generate(route))
                .stationName(SearchFilterUtil.normalize(request.getStationName()))
                .stationOrder(request.getStationOrder())
                .status(PredefinedMasterDataStatus.ACTIVE)
                .createdByAccountId(SecurityUtils.getRequiredCurrentAccountId())
                .build();

        return stationMapper.toStationResponse(stationRepository.save(station));
    }

    @Override
    @Transactional
    public StationResponse updateStation(Long stationId, UpdateStationRequest request) {
        validateStationId(stationId);
        Operator operator = securityUtils.getRequiredCurrentOperator();
        Station station = getStation(stationId, operator);
        Route route = getRoute(request.getRouteId(), operator);
        validateStationOrderAvailable(route, request.getStationOrder(), station.getId());

        station.setRoute(route);
        station.setStationName(SearchFilterUtil.normalize(request.getStationName()));
        station.setStationOrder(request.getStationOrder());
        return stationMapper.toStationResponse(stationRepository.save(station));
    }

    @Override
    @Transactional
    public StationResponse enableStation(Long stationId) {
        Station station = getStation(stationId, securityUtils.getRequiredCurrentOperator());
        if (PredefinedMasterDataStatus.ACTIVE.equals(station.getStatus())) {
            throw new AppException(ErrorCode.STATION_ALREADY_ENABLED);
        }
        station.setStatus(PredefinedMasterDataStatus.ACTIVE);
        return stationMapper.toStationResponse(stationRepository.save(station));
    }

    @Override
    @Transactional
    public StationResponse disableStation(Long stationId) {
        Station station = getStation(stationId, securityUtils.getRequiredCurrentOperator());
        if (PredefinedMasterDataStatus.DISABLED.equals(station.getStatus())) {
            throw new AppException(ErrorCode.STATION_ALREADY_DISABLED);
        }
        station.setStatus(PredefinedMasterDataStatus.DISABLED);
        return stationMapper.toStationResponse(stationRepository.save(station));
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

    private Station getStation(Long stationId, Operator operator) {
        validateStationId(stationId);
        return stationRepository.findByIdAndRouteOperatorId(stationId, operator.getId())
                .orElseGet(() -> {
                    if (stationRepository.existsById(stationId)) {
                        throw new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
                    }
                    throw new AppException(ErrorCode.STATION_NOT_FOUND);
                });
    }

    private void validateStationOrderAvailable(Route route, Integer stationOrder, Long currentStationId) {
        boolean existed = currentStationId == null
                ? stationRepository.existsByRouteAndStationOrder(route, stationOrder)
                : stationRepository.existsByRouteAndStationOrderAndIdNot(route, stationOrder, currentStationId);
        if (existed) {
            throw new AppException(ErrorCode.STATION_ORDER_EXISTED);
        }
    }

    private void validateStatus(String status) {
        if (status != null
                && !PredefinedMasterDataStatus.ACTIVE.equals(status)
                && !PredefinedMasterDataStatus.DISABLED.equals(status)) {
            throw new AppException(ErrorCode.INVALID_MASTER_DATA_STATUS);
        }
    }

    private void validateListParams(Long routeId, String keyword, String status, int page, int size) {
        if (routeId != null) {
            validateRouteId(routeId);
        }
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new AppException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }
        validateStatus(status);
    }

    private void validateRouteId(Long routeId) {
        if (routeId == null || routeId <= 0) {
            throw new AppException(ErrorCode.INVALID_ROUTE_ID);
        }
    }

    private void validateStationId(Long stationId) {
        if (stationId == null || stationId <= 0) {
            throw new AppException(ErrorCode.INVALID_STATION_ID);
        }
    }
}
