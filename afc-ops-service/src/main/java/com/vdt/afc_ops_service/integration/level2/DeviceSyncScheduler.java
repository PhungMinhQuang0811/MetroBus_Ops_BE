package com.vdt.afc_ops_service.integration.level2;

import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.mongo.ControlPackagePayloadRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class DeviceSyncScheduler {

    StationRepository stationRepository;
    StationControlSyncRepository syncRepository;
    ControlPackagePayloadRepository payloadRepository;
    ControlPackagePublisher controlPackagePublisher;

    @Scheduled(cron = "0 0 0 * * ?") // midnight every day
    public void syncAllDevices() {
        log.info("Starting midnight device sync...");
        List<Station> activeStations = stationRepository.findAllByStatus("ACTIVE");
        if (activeStations.isEmpty()) {
            log.info("No active stations found, skipping device sync");
            return;
        }

        int syncedCount = 0;
        for (Station station : activeStations) {
            try {
                Map<String, Object> combined = buildCombinedPayload(station);
                if (combined.size() > 1) { // more than just "publishedAt"
                    controlPackagePublisher.publishToStation(station.getStationCode(), combined);
                    syncedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to sync device for station {}: {}", station.getStationCode(), e.getMessage());
            }
        }
        log.info("Midnight device sync completed. Synced {} / {} stations", syncedCount, activeStations.size());
    }

    private Map<String, Object> buildCombinedPayload(Station station) {
        Map<String, Object> combined = new LinkedHashMap<>();
        combined.put("publishedAt", Instant.now().toString());

        // Đọc các syncs PENDING/APPLIED từ station_control_syncs (C3)
        List<StationControlSync> syncs = syncRepository
                .findByStationAndStatus(station.getStationCode(), List.of("PENDING", "APPLIED"));

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

        // Fallback: từ master data nếu chưa có STATION_CONTEXT
        if (stationContext == null) {
            stationContext = buildStationContextFallback(station);
        }

        if (deviceConfig != null) combined.put("deviceConfig", deviceConfig);
        if (stationContext != null) combined.put("stationContext", stationContext);
        if (mediaAccessRules != null) combined.put("mediaAccessRules", mediaAccessRules);

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

    private Map<String, Object> buildStationContextFallback(Station station) {
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