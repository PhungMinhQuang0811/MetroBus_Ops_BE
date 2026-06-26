package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.constant.PredefinedControlPackageSourceType;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageStatus;
import com.vdt.afc_ops_service.constant.PredefinedControlPackageType;
import com.vdt.afc_ops_service.document.ControlPackagePayload;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.ControlPackage;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.StationControlSync;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.ControlPackageRepository;
import com.vdt.afc_ops_service.repository.StationControlSyncRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.repository.mongo.ControlPackagePayloadRepository;
import com.vdt.afc_ops_service.service.IMediaAccessRulePackageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class MediaAccessRulePackageService implements IMediaAccessRulePackageService {

    CardRepository cardRepository;
    StationRepository stationRepository;
    ControlPackageRepository controlPackageRepository;
    ControlPackagePayloadRepository payloadRepository;
    StationControlSyncRepository syncRepository;

    @Override
    @Transactional
    public void refreshAndPublishForOperator(Operator operator) {
        List<Card> blockedCards = cardRepository.findByStatusIn(
                List.of("BLACKLISTED", "CANCELLED"));

        if (blockedCards.isEmpty()) {
            log.debug("No blocked cards for operator {}, skipping MEDIA_ACCESS_RULES", operator.getId());
            return;
        }

        Long maxVersion = controlPackageRepository.findMaxVersionForUpdate(operator.getId());
        long version = (maxVersion != null ? maxVersion : 0L) + 1;

        List<Map<String, Object>> cardStatusRules = blockedCards.stream()
                .map(card -> {
                    Map<String, Object> rule = new LinkedHashMap<>();
                    rule.put("cardId", card.getId());
                    rule.put("status", card.getStatus());
                    rule.put("statusReason", card.getStatusReason() != null ? card.getStatusReason() : "");
                    rule.put("updatedAt", card.getUpdatedAt() != null ? card.getUpdatedAt().toString() : "");
                    return rule;
                })
                .toList();

        Map<String, Object> payload = Map.of(
                "cardStatusRules", cardStatusRules
        );

        // Create control package
        ControlPackage controlPackage = ControlPackage.builder()
                .operator(operator)
                .version(version)
                .packageType(PredefinedControlPackageType.MEDIA_ACCESS_RULES)
                .sourceType(PredefinedControlPackageSourceType.LEVEL5_SYNCED)
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

        // Auto-publish to all ACTIVE stations of this operator
        List<Station> activeStations = stationRepository.findAllByStatusAndRouteOperatorId(
                "ACTIVE", operator.getId());

        int syncCount = 0;
        for (Station station : activeStations) {
            // Xóa MEDIA_ACCESS_RULES syncs cũ của station này để không trùng lặp
            syncRepository.deleteByStationIdAndPackageType(
                    station.getId(), PredefinedControlPackageType.MEDIA_ACCESS_RULES);

            StationControlSync sync = StationControlSync.builder()
                    .station(station)
                    .controlPackage(controlPackage)
                    .syncStatus("PENDING")
                    .retryCount(0)
                    .build();
            syncRepository.save(sync);
            syncCount++;
        }

        log.info("Created MEDIA_ACCESS_RULES package v{} for operator {} with {} cards, published to {} stations",
                version, operator.getId(), blockedCards.size(), syncCount);
    }
}