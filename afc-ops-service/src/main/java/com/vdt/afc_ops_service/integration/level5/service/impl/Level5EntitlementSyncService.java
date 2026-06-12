package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.integration.level5.dto.message.entitlement.C5EntitlementMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.entitlement.C5EntitlementUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Entitlement;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.EntitlementRepository;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5EntitlementSyncService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Level5EntitlementSyncService implements ILevel5EntitlementSyncService {

    CardRepository cardRepository;
    EntitlementRepository entitlementRepository;

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processEntitlement(C5EntitlementMessage message) {
        if (message == null || message.getTicketId() == null) {
            return rejected(null, null, "INVALID_TICKET_MESSAGE", "ticketId is required");
        }
        if (!PredefinedLevel5BusinessSync.MONTHLY_PASS.equals(normalizeUppercase(message.getType()))) {
            return ignored(message.getTicketId().toString(), null, "Entitlement sync ignores non monthly-pass messages");
        }

        Card card = message.getCardId() == null
                ? null
                : createCardPlaceholderIfMissing(message.getCardId());
        String entitlementId = message.getTicketId().toString();
        Long sourceVersion = toSourceVersion(message.getIssuedAt());
        Optional<Entitlement> existingEntitlement = entitlementRepository.findById(entitlementId);
        if (existingEntitlement.isPresent()
                && shouldIgnore(existingEntitlement.get().getSourceVersion(), sourceVersion)) {
            return ignored(entitlementId, existingEntitlement.get().getSourceVersion(), "Entitlement version ignored");
        }

        Entitlement entitlement = existingEntitlement.orElseGet(() -> Entitlement.builder().id(entitlementId).build());
        entitlement.setCard(card);
        entitlement.setFareProductCode(PredefinedLevel5BusinessSync.MONTHLY_PASS);
        entitlement.setPassPeriod(PredefinedLevel5BusinessSync.MONTH);
        entitlement.setPassScope(mapC5PassScope(message.getScope()));
        entitlement.setOperatorRef("*");
        entitlement.setRouteRef("*");
        entitlement.setFromStationRef(normalize(message.getFromStationCode()));
        entitlement.setToStationRef(normalize(message.getToStationCode()));
        entitlement.setTransportType(mapC5TransportType(message.getMode()));
        entitlement.setStatus(mapC5EntitlementStatus(message.getStatus()));
        entitlement.setValidFrom(toStartOfDay(message.getValidFrom()));
        entitlement.setValidTo(toStartOfDay(message.getValidTo()));
        entitlement.setSourceVersion(sourceVersion);
        entitlement.setSyncedAt(LocalDateTime.now());
        entitlementRepository.save(entitlement);

        return success(entitlementId,
                existingEntitlement.isPresent() ? PredefinedLevel5BusinessSync.UPDATED : PredefinedLevel5BusinessSync.CREATED,
                sourceVersion);
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processEntitlementSnapshot(C5TicketSyncMessage message) {
        if (message == null || message.getId() == null) {
            return rejected(null, null, "INVALID_TICKET_SYNC_MESSAGE", "id is required");
        }
        return processEntitlement(C5EntitlementMessage.builder()
                .ticketId(message.getId())
                .type(message.getType())
                .mode(message.getMode())
                .scope(message.getScope())
                .cardId(message.getCardId())
                .userId(message.getUserId())
                .fromStationCode(toRef(message.getFromStationId()))
                .toStationCode(toRef(message.getToStationId()))
                .fareAmount(message.getPrice())
                .status(message.getStatus())
                .validFrom(message.getValidFrom())
                .validTo(message.getValidTo())
                .issuedAt(message.getPurchasedAt())
                .build());
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processTicketUnlinked(C5EntitlementUnlinkedMessage message) {
        if (message == null || message.getTicketId() == null) {
            return rejected(null, null, "INVALID_TICKET_UNLINKED_MESSAGE", "ticketId is required");
        }

        String entitlementId = message.getTicketId().toString();
        Optional<Entitlement> entitlement = entitlementRepository.findById(entitlementId);
        if (entitlement.isEmpty()) {
            return ignored(entitlementId, null, "Entitlement sync ignores unlink for non-entitlement product");
        }

        Long sourceVersion = System.currentTimeMillis();
        Entitlement existingEntitlement = entitlement.get();
        existingEntitlement.setCard(null);
        existingEntitlement.setSourceVersion(sourceVersion);
        existingEntitlement.setSyncedAt(LocalDateTime.now());
        entitlementRepository.save(existingEntitlement);
        return success(entitlementId, PredefinedLevel5BusinessSync.UPDATED, sourceVersion);
    }

    private Card createCardPlaceholderIfMissing(UUID cardId) {
        String normalizedCardId = cardId.toString();
        return cardRepository.findById(normalizedCardId).orElseGet(() -> cardRepository.save(Card.builder()
                .id(normalizedCardId)
                .cardType(PredefinedLevel5BusinessSync.PHYSICAL)
                .status(PredefinedLevel5BusinessSync.ACTIVE)
                .sourceVersion(0L)
                .syncedAt(LocalDateTime.now())
                .build()));
    }

    private boolean shouldIgnore(Long currentVersion, Long incomingVersion) {
        return currentVersion != null && incomingVersion != null && currentVersion >= incomingVersion;
    }

    private String mapC5TransportType(String c5Mode) {
        String mode = normalizeUppercase(c5Mode);
        if (mode == null) {
            return PredefinedTransportType.METRO;
        }
        return switch (mode) {
            case "BUS" -> PredefinedTransportType.BUS;
            case "ANY" -> PredefinedLevel5BusinessSync.ALL;
            default -> PredefinedTransportType.METRO;
        };
    }

    private String mapC5PassScope(String c5Scope) {
        String scope = normalizeUppercase(c5Scope);
        if (scope == null) {
            return PredefinedLevel5BusinessSync.NETWORK;
        }
        return "MULTI_ROUTE".equals(scope)
                ? PredefinedLevel5BusinessSync.INTERLINE
                : PredefinedLevel5BusinessSync.SINGLE_ROUTE;
    }

    private String mapC5EntitlementStatus(String c5Status) {
        String status = normalizeUppercase(c5Status);
        if (status == null) {
            return PredefinedLevel5BusinessSync.ACTIVE;
        }
        return switch (status) {
            case "EXPIRED" -> PredefinedLevel5BusinessSync.EXPIRED;
            case "CANCELLED", "REVOKED" -> PredefinedLevel5BusinessSync.CANCELLED;
            default -> PredefinedLevel5BusinessSync.ACTIVE;
        };
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private Long toSourceVersion(Instant instant) {
        return instant == null ? System.currentTimeMillis() : instant.toEpochMilli();
    }

    private Level5BusinessSyncItemResult success(String externalId, String result, Long currentVersion) {
        return Level5BusinessSyncItemResult.builder()
                .externalId(externalId)
                .result(result)
                .currentVersion(currentVersion)
                .build();
    }

    private Level5BusinessSyncItemResult ignored(String externalId, Long currentVersion, String message) {
        return Level5BusinessSyncItemResult.builder()
                .externalId(externalId)
                .result(PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION)
                .currentVersion(currentVersion)
                .message(message)
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

    private String normalizeUppercase(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : normalized.toUpperCase();
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String toRef(UUID id) {
        return id == null ? null : id.toString();
    }
}
