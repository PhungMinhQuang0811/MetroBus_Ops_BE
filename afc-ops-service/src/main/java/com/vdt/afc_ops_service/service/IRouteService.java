package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.route.CreateRouteRequest;
import com.vdt.afc_ops_service.dto.request.route.UpdateRouteRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.route.RouteResponse;

public interface IRouteService {

    PageResponse<RouteResponse> listRoutes(String keyword, String transportType, String status, int page, int size);

    RouteResponse createRoute(CreateRouteRequest request);

    RouteResponse updateRoute(Long routeId, UpdateRouteRequest request);

    RouteResponse enableRoute(Long routeId);

    RouteResponse disableRoute(Long routeId);
}
