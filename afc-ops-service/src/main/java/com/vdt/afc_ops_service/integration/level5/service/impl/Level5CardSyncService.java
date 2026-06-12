package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5BlacklistMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardStatusMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5CardSyncService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Level5CardSyncService implements ILevel5CardSyncService {

    CardRepository cardRepository;

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processCardStatus(C5CardStatusMessage message) {
        if (message == null || message.getCardId() == null) {
            return rejected(null, null, "INVALID_CARD_STATUS_MESSAGE", "cardId is required");
        }

        String cardId = message.getCardId().toString();
        return upsertCard(
                cardId,
                normalize(message.getCardUid()),
                null,
                PredefinedLevel5BusinessSync.VIRTUAL_QR,
                mapC5CardStatus(message.getToStatus()),
                normalize(message.getReason()),
                toSourceVersion(message.getOccurredAt())
        );
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processCardSnapshot(C5CardSyncMessage message) {
        if (message == null || message.getId() == null) {
            return rejected(null, null, "INVALID_CARD_SYNC_MESSAGE", "id is required");
        }

        String cardId = message.getId().toString();
        return upsertCard(
                cardId,
                normalize(message.getCardUid()),
                toRef(message.getIssuedAtStationId()),
                PredefinedLevel5BusinessSync.VIRTUAL_QR,
                mapC5CardStatus(message.getStatus()),
                null,
                toSourceVersion(message.getUpdatedAt() == null ? message.getCreatedAt() : message.getUpdatedAt())
        );
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processBlacklist(String routingKey, C5BlacklistMessage message) {
        if (message == null || message.getCardId() == null) {
            return rejected(null, null, "INVALID_BLACKLIST_MESSAGE", "cardId is required");
        }

        boolean removed = "blacklist.removed".equals(routingKey)
                || "REMOVED".equals(normalizeUppercase(message.getAction()));
        String cardId = message.getCardId().toString();
        return upsertCard(
                cardId,
                null,
                null,
                PredefinedLevel5BusinessSync.VIRTUAL_QR,
                removed ? PredefinedLevel5BusinessSync.ACTIVE : PredefinedLevel5BusinessSync.BLACKLISTED,
                removed ? null : normalize(message.getReason()),
                toSourceVersion(message.getOccurredAt())
        );
    }

    private Level5BusinessSyncItemResult upsertCard(String cardId, String cardUid, String issuedAtStationRef, String cardType,
                                                    String status, String statusReason, Long sourceVersion) {
        Optional<Card> existingCard = cardRepository.findById(cardId);
        if (existingCard.isPresent() && shouldIgnore(existingCard.get().getSourceVersion(), sourceVersion)) {
            return ignored(cardId, existingCard.get().getSourceVersion(), sourceVersion);
        }

        Card card = existingCard.orElseGet(() -> Card.builder().id(cardId).build());
        if (cardUid != null) {
            card.setCardUid(cardUid);
        }
        if (issuedAtStationRef != null) {
            card.setIssuedAtStationRef(issuedAtStationRef);
        }
        card.setCardType(cardType);
        card.setStatus(status);
        card.setStatusReason(statusReason);
        card.setSourceVersion(sourceVersion);
        card.setSyncedAt(LocalDateTime.now());
        cardRepository.save(card);

        return success(cardId,
                existingCard.isPresent() ? PredefinedLevel5BusinessSync.UPDATED : PredefinedLevel5BusinessSync.CREATED,
                sourceVersion);
    }

    private boolean shouldIgnore(Long currentVersion, Long incomingVersion) {
        return currentVersion != null && incomingVersion != null && currentVersion >= incomingVersion;
    }

    private Level5BusinessSyncItemResult ignored(String externalId, Long currentVersion, Long incomingVersion) {
        return success(externalId,
                currentVersion != null && currentVersion.equals(incomingVersion)
                        ? PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION
                        : PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION,
                currentVersion);
    }

    private Level5BusinessSyncItemResult success(String externalId, String result, Long currentVersion) {
        return Level5BusinessSyncItemResult.builder()
                .externalId(externalId)
                .result(result)
                .currentVersion(currentVersion)
                .build();
    }

    private Level5BusinessSyncItemResult rejected(String externalId, Long currentVersion,
                                                  String errorCode, String message) {
        return Level5BusinessSyncItemResult.builder()
                .externalId(externalId)
                .result(PredefinedLevel5BusinessSync.REJECTED)
                .currentVersion(currentVersion)
                .errorCode(errorCode)
                .message(message)
                .build();
    }

    private String mapC5CardStatus(String c5Status) {
        String status = normalizeUppercase(c5Status);
        if (status == null) {
            return PredefinedLevel5BusinessSync.INACTIVE;
        }
        return switch (status) {
            case "ACTIVE" -> PredefinedLevel5BusinessSync.ACTIVE;
            case "REVOKED", "DISPOSED" -> PredefinedLevel5BusinessSync.CANCELLED;
            default -> PredefinedLevel5BusinessSync.INACTIVE;
        };
    }

    private Long toSourceVersion(Instant instant) {
        return instant == null ? System.currentTimeMillis() : instant.toEpochMilli();
    }

    private String toRef(java.util.UUID id) {
        return id == null ? null : id.toString();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeUppercase(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }
}
