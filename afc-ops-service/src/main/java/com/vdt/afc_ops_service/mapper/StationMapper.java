package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.dto.response.station.StationResponse;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import org.springframework.stereotype.Component;

@Component
public class StationMapper {

    public StationResponse toStationResponse(Station station) {
        Route route = station.getRoute();
        return StationResponse.builder()
                .id(station.getId())
                .routeId(route.getId())
                .routeCode(route.getRouteCode())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .stationOrder(station.getStationOrder())
                .status(station.getStatus())
                .createdAt(station.getCreatedAt())
                .updatedAt(station.getUpdatedAt())
                .build();
    }
}
