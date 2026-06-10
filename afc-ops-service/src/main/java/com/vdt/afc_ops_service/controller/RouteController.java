package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.dto.request.route.CreateRouteRequest;
import com.vdt.afc_ops_service.dto.request.route.UpdateRouteRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.route.ImportRouteConfirmResponse;
import com.vdt.afc_ops_service.dto.response.route.ImportRoutePreviewResponse;
import com.vdt.afc_ops_service.dto.response.route.RouteDetailResponse;
import com.vdt.afc_ops_service.dto.response.route.RouteResponse;
import com.vdt.afc_ops_service.service.IRouteService;
import com.vdt.afc_ops_service.service.Impl.RouteImportService;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/route")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RouteController {

    IRouteService routeService;
    RouteImportService routeImportService;

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

    @GetMapping("/get-route/{routeId}")
    public ApiResponse<RouteDetailResponse> getRoute(@PathVariable Long routeId) {
        return ApiResponse.<RouteDetailResponse>builder()
                .result(routeService.getRoute(routeId))
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

    @PostMapping("/preview-import-routes")
    public ApiResponse<ImportRoutePreviewResponse> previewImportRoutes(
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.<ImportRoutePreviewResponse>builder()
                .result(routeImportService.preview(resolveSingleImportFile(files)))
                .build();
    }

    @PostMapping("/confirm-import-routes")
    public ApiResponse<ImportRouteConfirmResponse> confirmImportRoutes(
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.<ImportRouteConfirmResponse>builder()
                .result(routeImportService.confirm(resolveSingleImportFile(files)))
                .build();
    }

    private MultipartFile resolveSingleImportFile(List<MultipartFile> files) {
        if (files == null || files.size() != 1) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }
        return files.get(0);
    }

}
