package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.document.ControlPackagePayload;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageStationSyncResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.PendingControlPackageResponse;
import com.vdt.afc_ops_service.entity.ControlPackage;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageAckResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackagePublishResponse;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageSourceType;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageStatus;
import com.vdt.afc_ops_service.dto.request.controlpackage.CreateControlPackageRequest;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
public class ControlPackageMapper {

    public ControlPackage toEntity(CreateControlPackageRequest request, Operator operator, Long version) {
        return ControlPackage.builder()
                .operator(operator)
                .version(version)
                .packageType(request.getPackageType())
                .sourceType(PredefinedControlPackageSourceType.LEVEL4_CREATED)
                .status(PredefinedControlPackageStatus.CREATED)
                .createdByAccountId(SecurityUtils.getCurrentAccountId())
                .build();
    }

    public ControlPackagePayload toDocument(CreateControlPackageRequest request, ControlPackage controlPackage, Long version) {
        return ControlPackagePayload.builder()
                .controlPackageId(controlPackage.getId())
                .packageType(controlPackage.getPackageType())
                .sourceType(controlPackage.getSourceType())
                .version(version)
                .payload(request.getPayload())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public StationControlSync toStationControlSync(Station station, ControlPackage controlPackage) {
        return StationControlSync.builder()
                .station(station)
                .controlPackage(controlPackage)
                .syncStatus("PENDING")
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public ControlPackagePublishResponse toPublishResponse(ControlPackage controlPackage, List<StationControlSync> syncs) {
        return ControlPackagePublishResponse.builder()
                .packageId(controlPackage.getId())
                .status(controlPackage.getStatus())
                .stationSyncs(syncs.stream().map(this::toStationSyncResponse).toList())
                .build();
    }

    public ControlPackageAckResponse toAckResponse(StationControlSync sync) {
        return ControlPackageAckResponse.builder()
                .syncId(sync.getId())
                .syncStatus(sync.getSyncStatus())
                .build();
    }

    public ControlPackageResponse toResponse(ControlPackage controlPackage) {
        return ControlPackageResponse.builder()
                .id(controlPackage.getId())
                .version(controlPackage.getVersion())
                .packageType(controlPackage.getPackageType())
                .sourceType(controlPackage.getSourceType())
                .status(controlPackage.getStatus())
                .createdAt(controlPackage.getCreatedAt())
                .updatedAt(controlPackage.getUpdatedAt())
                .build();
    }

    public ControlPackageDetailResponse toDetailResponse(ControlPackage controlPackage, Map<String, Object> payload) {
        return ControlPackageDetailResponse.builder()
                .id(controlPackage.getId())
                .version(controlPackage.getVersion())
                .packageType(controlPackage.getPackageType())
                .sourceType(controlPackage.getSourceType())
                .status(controlPackage.getStatus())
                .payload(payload)
                .createdByAccountId(controlPackage.getCreatedByAccountId())
                .createdAt(controlPackage.getCreatedAt())
                .updatedAt(controlPackage.getUpdatedAt())
                .publishedAt(controlPackage.getPublishedAt())
                .build();
    }

    public ControlPackageStationSyncResponse toStationSyncResponse(StationControlSync sync) {
        return ControlPackageStationSyncResponse.builder()
                .stationId(sync.getStation().getId())
                .syncStatus(sync.getSyncStatus())
                .build();
    }

    public PendingControlPackageResponse toPendingResponse(StationControlSync sync, ControlPackagePayload payload) {
        ControlPackage controlPackage = sync.getControlPackage();
        return PendingControlPackageResponse.builder()
                .syncId(sync.getId())
                .packageId(controlPackage.getId())
                .version(controlPackage.getVersion())
                .packageType(controlPackage.getPackageType())
                .sourceType(controlPackage.getSourceType())
                .payload(payload.getPayload())
                .build();
    }

    public ControlPackageSyncResponse toSyncResponse(StationControlSync sync) {
        Station station = sync.getStation();
        ControlPackage controlPackage = sync.getControlPackage();
        return ControlPackageSyncResponse.builder()
                .syncId(sync.getId())
                .stationId(station.getId())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .packageId(controlPackage.getId())
                .packageType(controlPackage.getPackageType())
                .version(controlPackage.getVersion())
                .syncStatus(sync.getSyncStatus())
                .retryCount(sync.getRetryCount())
                .lastAttemptAt(sync.getLastAttemptAt())
                .appliedAt(sync.getAppliedAt())
                .updatedAt(sync.getUpdatedAt())
                .errorMessage(sync.getErrorMessage())
                .build();
    }

    public ControlPackageSyncDetailResponse toSyncDetailResponse(StationControlSync sync) {
        Station station = sync.getStation();
        Route route = station.getRoute();
        ControlPackage controlPackage = sync.getControlPackage();
        return ControlPackageSyncDetailResponse.builder()
                .syncId(sync.getId())
                .stationId(station.getId())
                .stationCode(station.getStationCode())
                .stationName(station.getStationName())
                .routeId(route.getId())
                .routeName(route.getRouteName())
                .packageId(controlPackage.getId())
                .version(controlPackage.getVersion())
                .packageType(controlPackage.getPackageType())
                .sourceType(controlPackage.getSourceType())
                .packageStatus(controlPackage.getStatus())
                .syncStatus(sync.getSyncStatus())
                .retryCount(sync.getRetryCount())
                .lastAttemptAt(sync.getLastAttemptAt())
                .appliedAt(sync.getAppliedAt())
                .errorMessage(sync.getErrorMessage())
                .createdAt(sync.getCreatedAt())
                .updatedAt(sync.getUpdatedAt())
                .build();
    }
}
