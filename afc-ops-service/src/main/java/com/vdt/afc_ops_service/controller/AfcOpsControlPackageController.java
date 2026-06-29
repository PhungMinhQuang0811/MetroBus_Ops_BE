package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.controlpackage.AckControlPackageApplyRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.CreateControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.PublishControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.UpdateControlPackageRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageAckResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackagePublishResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.PendingControlPackageResponse;
import com.vdt.afc_ops_service.integration.level2.DeviceSyncService;
import com.vdt.afc_ops_service.service.IControlPackageService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/control-package")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AfcOpsControlPackageController {

    IControlPackageService controlPackageService;
    DeviceSyncService deviceSyncService;

    @PostMapping("/trigger-device-sync")
    public ApiResponse<Object> triggerDeviceSync(@RequestParam String stationCode) {
        return ApiResponse.builder()
                .result(deviceSyncService.triggerByStationCode(stationCode))
                .build();
    }

    @PostMapping("/create")
    public ApiResponse<ControlPackageResponse> create(
            @Valid @RequestBody CreateControlPackageRequest request
    ) {
        return ApiResponse.<ControlPackageResponse>builder()
                .result(controlPackageService.create(request))
                .build();
    }

    @PostMapping("/update/{packageId}")
    public ApiResponse<ControlPackageResponse> update(
            @PathVariable Long packageId,
            @Valid @RequestBody UpdateControlPackageRequest request
    ) {
        return ApiResponse.<ControlPackageResponse>builder()
                .result(controlPackageService.update(packageId, request))
                .build();
    }

    @GetMapping("/get-detail")
    public ApiResponse<ControlPackageDetailResponse> getDetail(
            @RequestParam Long packageId
    ) {
        return ApiResponse.<ControlPackageDetailResponse>builder()
                .result(controlPackageService.getDetail(packageId))
                .build();
    }

    @GetMapping("/list")
    public ApiResponse<PageResponse<ControlPackageResponse>> list(
            @RequestParam(required = false) String packageType,
            @RequestParam(required = false) String sourceType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<ControlPackageResponse>>builder()
                .result(controlPackageService.list(packageType, sourceType, status, page, size))
                .build();
    }

    @PostMapping("/publish/{packageId}")
    public ApiResponse<ControlPackagePublishResponse> publish(
            @PathVariable Long packageId,
            @Valid @RequestBody PublishControlPackageRequest request
    ) {
        return ApiResponse.<ControlPackagePublishResponse>builder()
                .result(controlPackageService.publish(packageId, request))
                .build();
    }

    @GetMapping("/pull-pending")
    public ApiResponse<List<PendingControlPackageResponse>> pullPending(
            @RequestParam String stationCode,
            @RequestParam Long currentVersion
    ) {
        return ApiResponse.<List<PendingControlPackageResponse>>builder()
                .result(controlPackageService.pullPending(stationCode, currentVersion))
                .build();
    }

    @PostMapping("/ack-apply/{syncId}")
    public ApiResponse<ControlPackageAckResponse> ackApply(
            @PathVariable Long syncId,
            @Valid @RequestBody AckControlPackageApplyRequest request
    ) {
        return ApiResponse.<ControlPackageAckResponse>builder()
                .result(controlPackageService.ackApply(syncId, request))
                .build();
    }

    @GetMapping("/search-syncs")
    public ApiResponse<PageResponse<ControlPackageSyncResponse>> searchSyncs(
            @RequestParam(required = false) String packageType,
            @RequestParam(required = false) Long version,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<ControlPackageSyncResponse>>builder()
                .result(controlPackageService.searchSyncs(packageType, version, stationId, status, page, size))
                .build();
    }

    @GetMapping("/get-sync-detail")
    public ApiResponse<ControlPackageSyncDetailResponse> getSyncDetail(
            @RequestParam Long syncId
    ) {
        return ApiResponse.<ControlPackageSyncDetailResponse>builder()
                .result(controlPackageService.getSyncDetail(syncId))
                .build();
    }
}
