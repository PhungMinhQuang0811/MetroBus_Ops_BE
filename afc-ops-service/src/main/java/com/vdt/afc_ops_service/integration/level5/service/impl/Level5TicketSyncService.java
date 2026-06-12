package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.TicketRepository;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5TicketSyncService;
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
public class Level5TicketSyncService implements ILevel5TicketSyncService {

    CardRepository cardRepository;
    TicketRepository ticketRepository;

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processTicket(C5TicketMessage message) {
        if (message == null || message.getTicketId() == null) {
            return rejected(null, null, "INVALID_TICKET_MESSAGE", "ticketId is required");
        }
        if (!"SINGLE_TRIP".equals(normalizeUppercase(message.getType()))) {
            return ignored(message.getTicketId().toString(), null, "Ticket sync ignores non single-trip messages");
        }

        Card card = message.getCardId() == null
                ? null
                : createCardPlaceholderIfMissing(message.getCardId());
        String ticketId = message.getTicketId().toString();
        Long sourceVersion = toSourceVersion(message.getIssuedAt());
        Optional<Ticket> existingTicket = ticketRepository.findById(ticketId);
        if (existingTicket.isPresent() && shouldIgnore(existingTicket.get().getSourceVersion(), sourceVersion)) {
            return ignored(ticketId, existingTicket.get().getSourceVersion(), "Ticket version ignored");
        }

        Ticket ticket = existingTicket.orElseGet(() -> Ticket.builder().id(ticketId).build());
        ticket.setCard(card);
        ticket.setTicketType(PredefinedLevel5BusinessSync.METRO_SINGLE_RIDE);
        ticket.setRouteScopeType(PredefinedLevel5BusinessSync.NETWORK);
        ticket.setOperatorRef("*");
        ticket.setRouteRef("*");
        ticket.setFromStationRef(normalize(message.getFromStationCode()));
        ticket.setToStationRef(normalize(message.getToStationCode()));
        ticket.setTransportType(mapC5TransportType(message.getMode()));
        ticket.setUsageStatus(mapC5TicketStatus(message.getStatus()));
        ticket.setValidFrom(toStartOfDay(message.getValidFrom()));
        ticket.setValidTo(toStartOfDay(message.getValidTo()));
        ticket.setUsedAt(toLocalDateTime(message.getUsedAt()));
        ticket.setSourceVersion(sourceVersion);
        ticket.setSyncedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        return success(ticketId,
                existingTicket.isPresent() ? PredefinedLevel5BusinessSync.UPDATED : PredefinedLevel5BusinessSync.CREATED,
                sourceVersion);
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processTicketSnapshot(C5TicketSyncMessage message) {
        if (message == null || message.getId() == null) {
            return rejected(null, null, "INVALID_TICKET_SYNC_MESSAGE", "id is required");
        }
        return processTicket(C5TicketMessage.builder()
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
                .usedAt(message.getUsedAt())
                .issuedAt(message.getPurchasedAt())
                .build());
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processTicketUnlinked(C5TicketUnlinkedMessage message) {
        if (message == null || message.getTicketId() == null) {
            return rejected(null, null, "INVALID_TICKET_UNLINKED_MESSAGE", "ticketId is required");
        }

        String ticketId = message.getTicketId().toString();
        Optional<Ticket> ticket = ticketRepository.findById(ticketId);
        if (ticket.isEmpty()) {
            return ignored(ticketId, null, "Ticket sync ignores unlink for non-ticket product");
        }

        Long sourceVersion = System.currentTimeMillis();
        Ticket existingTicket = ticket.get();
        existingTicket.setCard(null);
        existingTicket.setSourceVersion(sourceVersion);
        existingTicket.setSyncedAt(LocalDateTime.now());
        ticketRepository.save(existingTicket);
        return success(ticketId, PredefinedLevel5BusinessSync.UPDATED, sourceVersion);
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

    private String mapC5TicketStatus(String c5Status) {
        String status = normalizeUppercase(c5Status);
        if (status == null) {
            return PredefinedLevel5BusinessSync.UNUSED;
        }
        return switch (status) {
            case "USED" -> PredefinedLevel5BusinessSync.USED;
            case "EXPIRED" -> PredefinedLevel5BusinessSync.EXPIRED;
            case "CANCELLED", "REVOKED" -> PredefinedLevel5BusinessSync.CANCELLED;
            default -> PredefinedLevel5BusinessSync.UNUSED;
        };
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
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
