package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.dto.response.route.RouteDetailResponse;
import com.vdt.afc_ops_service.dto.response.route.RouteResponse;
import com.vdt.afc_ops_service.dto.response.station.StationResponse;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteMapper {

    public RouteResponse toRouteResponse(Route route) {
        return RouteResponse.builder()
                .id(route.getId())
                .operatorId(route.getOperator().getId())
                .routeCode(route.getRouteCode())
                .routeName(route.getRouteName())
                .transportType(route.getTransportType())
                .status(route.getStatus())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .build();
    }

    public RouteDetailResponse toRouteDetailResponse(Route route, List<Station> stations) {
        var stationResponses = stations.stream()
                .map(this::toStationResponse)
                .toList();

        return RouteDetailResponse.builder()
                .id(route.getId())
                .operatorId(route.getOperator().getId())
                .routeCode(route.getRouteCode())
                .routeName(route.getRouteName())
                .transportType(route.getTransportType())
                .status(route.getStatus())
                .createdAt(route.getCreatedAt())
                .updatedAt(route.getUpdatedAt())
                .stationCount(stationResponses.size())
                .stations(stationResponses)
                .build();
    }

    public StationResponse toStationResponse(Station station) {
        return StationResponse.builder()
                .id(station.getId())
                .routeId(station.getRoute().getId())
                .routeCode(station.getRoute().getRouteCode())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .stationOrder(station.getStationOrder())
                .status(station.getStatus())
                .createdAt(station.getCreatedAt())
                .updatedAt(station.getUpdatedAt())
                .build();
    }
}
