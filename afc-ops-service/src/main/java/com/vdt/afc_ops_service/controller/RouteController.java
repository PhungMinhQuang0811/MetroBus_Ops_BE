package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.route.CreateRouteRequest;
import com.vdt.afc_ops_service.dto.request.route.UpdateRouteRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.route.RouteResponse;
import com.vdt.afc_ops_service.service.IRouteService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/afc-ops")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RouteController {

    IRouteService routeService;

    @GetMapping("/list-routes")
    public ApiResponse<PageResponse<RouteResponse>> listRoutes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String transportType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<RouteResponse>>builder()
                .result(routeService.listRoutes(keyword, transportType, status, page, size))
                .build();
    }

    @PostMapping("/create-route")
    public ApiResponse<RouteResponse> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        return ApiResponse.<RouteResponse>builder()
                .result(routeService.createRoute(request))
                .build();
    }

    @PostMapping("/update-route/{routeId}")
    public ApiResponse<RouteResponse> updateRoute(
            @PathVariable Long routeId,
            @Valid @RequestBody UpdateRouteRequest request
    ) {
        return ApiResponse.<RouteResponse>builder()
                .result(routeService.updateRoute(routeId, request))
                .build();
    }

    @PostMapping("/enable-route/{routeId}")
    public ApiResponse<RouteResponse> enableRoute(@PathVariable Long routeId) {
        return ApiResponse.<RouteResponse>builder()
                .result(routeService.enableRoute(routeId))
                .build();
    }

    @PostMapping("/disable-route/{routeId}")
    public ApiResponse<RouteResponse> disableRoute(@PathVariable Long routeId) {
        return ApiResponse.<RouteResponse>builder()
                .result(routeService.disableRoute(routeId))
                .build();
    }

}
