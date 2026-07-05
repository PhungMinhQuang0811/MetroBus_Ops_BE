package com.vdt.afc_ops_service.integration.level2;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.mongo.ControlPackagePayloadRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DeviceSyncService {

    StationRepository stationRepository;
    StationControlSyncRepository syncRepository;
    ControlPackagePayloadRepository payloadRepository;
    ControlPackagePublisher controlPackagePublisher;

    /**
     * Publish combined control package (device config + station context + media access rules)
     * for a specific station to C2 via RabbitMQ immediately.
     */
    public Map<String, Object> triggerByStationCode(String stationCode) {
        String normalizedCode = SearchFilterUtil.normalize(stationCode);
        if (normalizedCode == null) {
            throw new AppException(ErrorCode.FIELD_REQUIRED);
        }

        List<Station> activeStations = stationRepository.findAllByStatus("ACTIVE");
        Station station = activeStations.stream()
                .filter(s -> normalizedCode.equals(s.getStationCode()))
                .findFirst()
                .orElse(null);
        if (station == null) {
            throw new AppException(ErrorCode.STATION_NOT_FOUND);
        }

        Map<String, Object> combined = new LinkedHashMap<>();
        combined.put("publishedAt", Instant.now().toString());

        List<StationControlSync> syncs = syncRepository
                .findByStationAndStatus(normalizedCode, List.of("PENDING", "APPLIED"));

        Map<String, Object> deviceConfig = null;
        Map<String, Object> stationContext = null;
        Map<String, Object> mediaAccessRules = null;

        for (StationControlSync sync : syncs) {
            String pkgType = sync.getControlPackage().getPackageType();
            if (deviceConfig == null && "DEVICE_CONFIG".equals(pkgType)) {
                deviceConfig = loadPayloadFromSync(sync);
            } else if (stationContext == null && "STATION_CONTEXT".equals(pkgType)) {
                stationContext = loadPayloadFromSync(sync);
            } else if (mediaAccessRules == null && "MEDIA_ACCESS_RULES".equals(pkgType)) {
                mediaAccessRules = loadPayloadFromSync(sync);
            }
        }

        if (stationContext == null) {
            stationContext = buildStationContextFromMasterData(station);
        }

        if (deviceConfig != null) combined.put("deviceConfig", deviceConfig);
        if (stationContext != null) combined.put("stationContext", stationContext);
        if (mediaAccessRules != null) combined.put("mediaAccessRules", mediaAccessRules);

        controlPackagePublisher.publishToStation(normalizedCode, combined);
        log.info("Manual trigger: published control package to station {}", normalizedCode);
        return combined;
    }

    private Map<String, Object> loadPayloadFromSync(StationControlSync sync) {
        var payloadDoc = payloadRepository.findByControlPackageId(sync.getControlPackage().getId());
        if (payloadDoc.isEmpty()) return null;

        Map<String, Object> result = new LinkedHashMap<>(payloadDoc.get().getPayload());
        result.put("version", sync.getControlPackage().getVersion());
        if (sync.getId() != null) {
            result.put("syncId", sync.getId());
        }
        return result;
    }

    private Map<String, Object> buildStationContextFromMasterData(Station station) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("stationCode", station.getStationCode());
        context.put("stationName", station.getStationName() != null ? station.getStationName() : "");
        context.put("routeCode", station.getRoute() != null ? station.getRoute().getRouteCode() : "");
        context.put("stationOrder", station.getStationOrder());
        context.put("distance", station.getDistance() != null ? station.getDistance() : java.math.BigDecimal.ZERO);
        context.put("operatorCode", station.getRoute() != null && station.getRoute().getOperator() != null
                ? station.getRoute().getOperator().getOperatorCode() : "");
        context.put("version", 0);
        return context;
    }
}