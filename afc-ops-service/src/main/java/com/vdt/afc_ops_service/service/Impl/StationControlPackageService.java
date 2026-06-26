package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.constant.PredefinedControlPackageSourceType;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageStatus;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageType;
import com.vdt.afc_ops_service.document.ControlPackagePayload;
import com.vdt.afc_ops_service.entity.ControlPackage;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.repository.ControlPackageRepository;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.mongo.ControlPackagePayloadRepository;
import com.vdt.afc_ops_service.service.IStationControlPackageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StationControlPackageService implements IStationControlPackageService {

    ControlPackageRepository controlPackageRepository;
    ControlPackagePayloadRepository payloadRepository;
    StationControlSyncRepository syncRepository;

    @Override
    @Transactional
    public void createOrUpdateStationContext(Station station) {
        Operator operator = station.getRoute().getOperator();
        Long maxVersion = controlPackageRepository.findMaxVersionForUpdate(operator.getId());
        long version = (maxVersion != null ? maxVersion : 0L) + 1;

        String stationCode = station.getStationCode();
        String routeCode = station.getRoute() != null ? station.getRoute().getRouteCode() : null;

        Map<String, Object> payload = Map.of(
                "stationCode", stationCode,
                "stationName", station.getStationName() != null ? station.getStationName() : "",
                "routeCode", routeCode != null ? routeCode : "",
                "stationOrder", station.getStationOrder(),
                "distance", station.getDistance() != null ? station.getDistance() : java.math.BigDecimal.ZERO,
                "operatorCode", operator.getOperatorCode()
        );

        // Create control package
        ControlPackage controlPackage = ControlPackage.builder()
                .operator(operator)
                .version(version)
                .packageType(PredefinedControlPackageType.STATION_CONTEXT)
                .sourceType(PredefinedControlPackageSourceType.LEVEL4_CREATED)
                .status(PredefinedControlPackageStatus.PUBLISHED)
                .publishedAt(LocalDateTime.now())
                .createdByAccountId(null)
                .build();
        controlPackage = controlPackageRepository.save(controlPackage);

        // Save payload to MongoDB
        ControlPackagePayload mongoPayload = ControlPackagePayload.builder()
                .controlPackageId(controlPackage.getId())
                .packageType(controlPackage.getPackageType())
                .sourceType(controlPackage.getSourceType())
                .version(version)
                .payload(payload)
                .createdAt(LocalDateTime.now())
                .build();
        payloadRepository.save(mongoPayload);
        controlPackage.setPayloadRef(mongoPayload.getId());
        controlPackageRepository.save(controlPackage);

        // Xóa STATION_CONTEXT syncs cũ của station này để không trùng lặp
        syncRepository.deleteByStationIdAndPackageType(
                station.getId(), PredefinedControlPackageType.STATION_CONTEXT);

        // Auto-publish to this station
        StationControlSync sync = StationControlSync.builder()
                .station(station)
                .controlPackage(controlPackage)
                .syncStatus("PENDING")
                .retryCount(0)
                .build();
        syncRepository.save(sync);

        log.info("Created STATION_CONTEXT package v{} for station {}", version, stationCode);
    }
}