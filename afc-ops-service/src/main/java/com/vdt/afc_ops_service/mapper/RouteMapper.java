package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.dto.response.route.RouteResponse;
import com.vdt.afc_ops_service.entity.Route;
import org.springframework.stereotype.Component;

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
}
