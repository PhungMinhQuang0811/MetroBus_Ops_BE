package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5BlacklistMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardStatusMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5BusinessSyncItemMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5BusinessSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5CardPayload;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5EntitlementPayload;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5TicketPayload;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncResult;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Entitlement;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.integration.level5.mapper.Level5BusinessSyncMapper;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.EntitlementRepository;
import com.vdt.afc_ops_service.repository.TicketRepository;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5BusinessSyncService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Level5BusinessSyncService implements ILevel5BusinessSyncService {

    CardRepository cardRepository;
    TicketRepository ticketRepository;
    EntitlementRepository entitlementRepository;
    Level5BusinessSyncMapper mapper;

    @Override
    @Transactional
    public Level5BusinessSyncResult processSync(Level5BusinessSyncMessage message) {
        String syncType = normalizeUppercase(message == null ? null : message.getSyncType());
        List<Level5BusinessSyncItemResult> itemResults = new ArrayList<>();

        if (!PredefinedLevel5BusinessSync.SYNC_TYPES.contains(syncType)) {
            itemResults.add(rejected(null, null, "INVALID_SYNC_TYPE", "Unsupported sync type"));
            return buildResult(syncType, message == null ? null : message.getCorrelationId(), itemResults);
        }

        List<Level5BusinessSyncItemMessage> items = message.getItems();
        if (items == null || items.isEmpty()) {
            itemResults.add(rejected(null, null, "EMPTY_ITEMS", "Sync message must contain at least one item"));
            return buildResult(syncType, message.getCorrelationId(), itemResults);
        }

        for (Level5BusinessSyncItemMessage item : items) {
            itemResults.add(processItem(syncType, item));
        }

        return buildResult(syncType, message.getCorrelationId(), itemResults);
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processC5CardStatus(C5CardStatusMessage message) {
        if (message == null || message.getCardId() == null) {
            return rejected(null, null, "INVALID_CARD_STATUS_MESSAGE", "cardId is required");
        }

        String cardId = message.getCardId().toString();
        Long sourceVersion = toSourceVersion(message.getOccurredAt());
        Level5CardPayload payload = Level5CardPayload.builder()
                .cardId(cardId)
                .cardUid(normalize(message.getCardUid()))
                .cardType(PredefinedLevel5BusinessSync.PHYSICAL)
                .status(mapC5CardStatus(message.getToStatus()))
                .statusReason(normalize(message.getReason()))
                .build();

        return processCard(Level5BusinessSyncItemMessage.builder()
                .externalId(cardId)
                .sourceVersion(sourceVersion)
                .card(payload)
                .build());
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processC5Blacklist(String routingKey, C5BlacklistMessage message) {
        if (message == null || message.getCardId() == null) {
            return rejected(null, null, "INVALID_BLACKLIST_MESSAGE", "cardId is required");
        }

        String cardId = message.getCardId().toString();
        boolean removed = "blacklist.removed".equals(routingKey)
                || "REMOVED".equals(normalizeUppercase(message.getAction()));
        Long sourceVersion = toSourceVersion(message.getOccurredAt());
        Level5CardPayload payload = Level5CardPayload.builder()
                .cardId(cardId)
                .cardType(PredefinedLevel5BusinessSync.PHYSICAL)
                .status(removed ? PredefinedLevel5BusinessSync.ACTIVE : PredefinedLevel5BusinessSync.BLACKLISTED)
                .statusReason(removed ? null : normalize(message.getReason()))
                .build();

        return processCard(Level5BusinessSyncItemMessage.builder()
                .externalId(cardId)
                .sourceVersion(sourceVersion)
                .card(payload)
                .build());
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processC5Ticket(C5TicketMessage message) {
        if (message == null || message.getTicketId() == null) {
            return rejected(null, null, "INVALID_TICKET_MESSAGE", "ticketId is required");
        }

        if (message.getCardId() != null) {
            createCardPlaceholderIfMissing(message.getCardId());
        }

        String ticketType = normalizeUppercase(message.getType());
        if ("SINGLE_TRIP".equals(ticketType)) {
            return processC5SingleTripTicket(message);
        }
        if (PredefinedLevel5BusinessSync.MONTHLY_PASS.equals(ticketType)) {
            return processC5MonthlyPass(message);
        }
        return rejected(message.getTicketId().toString(), toSourceVersion(message.getIssuedAt()),
                "INVALID_TICKET_TYPE", "Unsupported C5 ticket type");
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processC5TicketUnlinked(C5TicketUnlinkedMessage message) {
        if (message == null || message.getTicketId() == null) {
            return rejected(null, null, "INVALID_TICKET_UNLINKED_MESSAGE", "ticketId is required");
        }

        String ticketId = message.getTicketId().toString();
        Long sourceVersion = System.currentTimeMillis();

        Optional<Ticket> ticket = ticketRepository.findById(ticketId);
        if (ticket.isPresent()) {
            Ticket existingTicket = ticket.get();
            existingTicket.setCard(null);
            existingTicket.setSourceVersion(sourceVersion);
            existingTicket.setSyncedAt(LocalDateTime.now());
            ticketRepository.save(existingTicket);
            return success(ticketId, PredefinedLevel5BusinessSync.UPDATED, sourceVersion);
        }

        Optional<Entitlement> entitlement = entitlementRepository.findById(ticketId);
        if (entitlement.isPresent()) {
            Entitlement existingEntitlement = entitlement.get();
            existingEntitlement.setCard(null);
            existingEntitlement.setSourceVersion(sourceVersion);
            existingEntitlement.setSyncedAt(LocalDateTime.now());
            entitlementRepository.save(existingEntitlement);
            return success(ticketId, PredefinedLevel5BusinessSync.UPDATED, sourceVersion);
        }

        return rejected(ticketId, null, "PRODUCT_NOT_SYNCED", "Ticket or entitlement has not been synced");
    }

    private Level5BusinessSyncItemResult processItem(String syncType, Level5BusinessSyncItemMessage item) {
        if (item == null) {
            return rejected(null, null, "INVALID_ITEM", "Sync item must not be null");
        }
        if (item.getSourceVersion() == null || item.getSourceVersion() < 0) {
            return rejected(item.getExternalId(), null, "INVALID_SOURCE_VERSION", "sourceVersion must be non-negative");
        }

        return switch (syncType) {
            case PredefinedLevel5BusinessSync.CARD_UPSERT,
                 PredefinedLevel5BusinessSync.CARD_STATUS_CHANGED -> processCard(item);
            case PredefinedLevel5BusinessSync.TICKET_UPSERT,
                 PredefinedLevel5BusinessSync.TICKET_STATUS_CHANGED -> processTicket(item);
            case PredefinedLevel5BusinessSync.ENTITLEMENT_UPSERT,
                 PredefinedLevel5BusinessSync.ENTITLEMENT_STATUS_CHANGED -> processEntitlement(item);
            default -> rejected(item.getExternalId(), item.getSourceVersion(),
                    "INVALID_SYNC_TYPE", "Unsupported sync type");
        };
    }

    private Level5BusinessSyncItemResult processCard(Level5BusinessSyncItemMessage item) {
        Level5CardPayload payload = item.getCard();
        if (payload == null) {
            return rejected(item.getExternalId(), item.getSourceVersion(),
                    "CARD_PAYLOAD_REQUIRED", "card payload is required");
        }

        String cardId = normalize(payload.getCardId());
        if (isBlank(cardId) || !externalIdMatches(item.getExternalId(), cardId)) {
            return rejected(item.getExternalId(), item.getSourceVersion(),
                    "INVALID_CARD_ID", "cardId is required and must match externalId when provided");
        }
        payload.setCardId(cardId);
        payload.setCardType(normalizeUppercase(payload.getCardType()));
        payload.setStatus(normalizeUppercase(payload.getStatus()));

        String validationError = validateCard(payload);
        if (validationError != null) {
            return rejected(cardId, item.getSourceVersion(), validationError, "Invalid card payload");
        }

        Optional<Card> existingCard = cardRepository.findById(cardId);
        VersionDecision versionDecision = decideVersion(existingCard.map(Card::getSourceVersion), item.getSourceVersion());
        if (versionDecision.isIgnored()) {
            return ignored(cardId, versionDecision.result(), existingCard.map(Card::getSourceVersion).orElse(null));
        }

        LocalDateTime syncedAt = LocalDateTime.now();
        Card card = existingCard.orElseGet(() -> mapper.toCard(payload, item.getSourceVersion(), syncedAt));
        if (existingCard.isPresent()) {
            mapper.updateCard(card, payload, item.getSourceVersion(), syncedAt);
        }
        cardRepository.save(card);
        return success(cardId, versionDecision.result(), item.getSourceVersion());
    }

    private Level5BusinessSyncItemResult processTicket(Level5BusinessSyncItemMessage item) {
        Level5TicketPayload payload = item.getTicket();
        if (payload == null) {
            return rejected(item.getExternalId(), item.getSourceVersion(),
                    "TICKET_PAYLOAD_REQUIRED", "ticket payload is required");
        }

        String ticketId = normalize(payload.getTicketId());
        String cardId = normalize(payload.getCardId());
        if (isBlank(ticketId) || !externalIdMatches(item.getExternalId(), ticketId)) {
            return rejected(item.getExternalId(), item.getSourceVersion(),
                    "INVALID_TICKET_ID", "ticketId is required and must match externalId when provided");
        }
        payload.setTicketId(ticketId);
        payload.setCardId(cardId);
        normalizeTicketPayload(payload);

        String validationError = validateTicket(payload);
        if (validationError != null) {
            return rejected(ticketId, item.getSourceVersion(), validationError, "Invalid ticket payload");
        }

        Card card = null;
        if (!isBlank(cardId)) {
            Optional<Card> existingCard = cardRepository.findById(cardId);
            if (existingCard.isEmpty()) {
                return rejected(ticketId, item.getSourceVersion(), "CARD_NOT_SYNCED", "Referenced card has not been synced");
            }
            card = existingCard.get();
        }

        Optional<Ticket> existingTicket = ticketRepository.findById(ticketId);
        VersionDecision versionDecision = decideVersion(existingTicket.map(Ticket::getSourceVersion), item.getSourceVersion());
        if (versionDecision.isIgnored()) {
            return ignored(ticketId, versionDecision.result(), existingTicket.map(Ticket::getSourceVersion).orElse(null));
        }

        LocalDateTime syncedAt = LocalDateTime.now();
        Card ticketCard = card;
        Ticket ticket = existingTicket.orElseGet(() -> mapper.toTicket(payload, ticketCard, item.getSourceVersion(), syncedAt));
        if (existingTicket.isPresent()) {
            mapper.updateTicket(ticket, payload, ticketCard, item.getSourceVersion(), syncedAt);
        }
        ticketRepository.save(ticket);
        return success(ticketId, versionDecision.result(), item.getSourceVersion());
    }

    private Level5BusinessSyncItemResult processEntitlement(Level5BusinessSyncItemMessage item) {
        Level5EntitlementPayload payload = item.getEntitlement();
        if (payload == null) {
            return rejected(item.getExternalId(), item.getSourceVersion(),
                    "ENTITLEMENT_PAYLOAD_REQUIRED", "entitlement payload is required");
        }

        String entitlementId = normalize(payload.getEntitlementId());
        String cardId = normalize(payload.getCardId());
        if (isBlank(entitlementId) || !externalIdMatches(item.getExternalId(), entitlementId)) {
            return rejected(item.getExternalId(), item.getSourceVersion(),
                    "INVALID_ENTITLEMENT_ID", "entitlementId is required and must match externalId when provided");
        }
        payload.setEntitlementId(entitlementId);
        payload.setCardId(cardId);
        normalizeEntitlementPayload(payload);

        String validationError = validateEntitlement(payload);
        if (validationError != null) {
            return rejected(entitlementId, item.getSourceVersion(), validationError, "Invalid entitlement payload");
        }

        Card card = null;
        if (!isBlank(cardId)) {
            Optional<Card> existingCard = cardRepository.findById(cardId);
            if (existingCard.isEmpty()) {
                return rejected(entitlementId, item.getSourceVersion(),
                        "CARD_NOT_SYNCED", "Referenced card has not been synced");
            }
            card = existingCard.get();
        }

        Optional<Entitlement> existingEntitlement = entitlementRepository.findById(entitlementId);
        VersionDecision versionDecision = decideVersion(
                existingEntitlement.map(Entitlement::getSourceVersion),
                item.getSourceVersion()
        );
        if (versionDecision.isIgnored()) {
            return ignored(entitlementId, versionDecision.result(),
                    existingEntitlement.map(Entitlement::getSourceVersion).orElse(null));
        }

        LocalDateTime syncedAt = LocalDateTime.now();
        Card entitlementCard = card;
        Entitlement entitlement = existingEntitlement.orElseGet(() ->
                mapper.toEntitlement(payload, entitlementCard, item.getSourceVersion(), syncedAt));
        if (existingEntitlement.isPresent()) {
            mapper.updateEntitlement(entitlement, payload, entitlementCard, item.getSourceVersion(), syncedAt);
        }
        entitlementRepository.save(entitlement);
        return success(entitlementId, versionDecision.result(), item.getSourceVersion());
    }

    private Level5BusinessSyncItemResult processC5SingleTripTicket(C5TicketMessage message) {
        Level5TicketPayload payload = Level5TicketPayload.builder()
                .ticketId(message.getTicketId().toString())
                .cardId(message.getCardId() == null ? null : message.getCardId().toString())
                .ticketType(PredefinedLevel5BusinessSync.METRO_SINGLE_RIDE)
                .routeScopeType(PredefinedLevel5BusinessSync.NETWORK)
                .operatorRef("*")
                .routeRef("*")
                .transportType(mapC5TransportType(message.getMode()))
                .usageStatus(mapC5TicketStatus(message.getStatus()))
                .validFrom(toStartOfDay(message.getValidFrom()))
                .validTo(toStartOfDay(message.getValidTo()))
                .usedAt(toLocalDateTime(message.getUsedAt()))
                .build();

        return processTicket(Level5BusinessSyncItemMessage.builder()
                .externalId(message.getTicketId().toString())
                .sourceVersion(toSourceVersion(message.getIssuedAt()))
                .ticket(payload)
                .build());
    }

    private Level5BusinessSyncItemResult processC5MonthlyPass(C5TicketMessage message) {
        Level5EntitlementPayload payload = Level5EntitlementPayload.builder()
                .entitlementId(message.getTicketId().toString())
                .cardId(message.getCardId() == null ? null : message.getCardId().toString())
                .fareProductCode(PredefinedLevel5BusinessSync.MONTHLY_PASS)
                .passPeriod(PredefinedLevel5BusinessSync.MONTH)
                .passScope(mapC5PassScope(message.getScope()))
                .operatorRef("*")
                .routeRef("*")
                .transportType(mapC5TransportType(message.getMode()))
                .passengerType(null)
                .status(mapC5EntitlementStatus(message.getStatus()))
                .validFrom(toStartOfDay(message.getValidFrom()))
                .validTo(toStartOfDay(message.getValidTo()))
                .build();

        return processEntitlement(Level5BusinessSyncItemMessage.builder()
                .externalId(message.getTicketId().toString())
                .sourceVersion(toSourceVersion(message.getIssuedAt()))
                .entitlement(payload)
                .build());
    }

    private String validateCard(Level5CardPayload payload) {
        if (!PredefinedLevel5BusinessSync.CARD_TYPES.contains(payload.getCardType())) {
            return "INVALID_CARD_TYPE";
        }
        if (!PredefinedLevel5BusinessSync.CARD_STATUSES.contains(payload.getStatus())) {
            return "INVALID_CARD_STATUS";
        }
        return null;
    }

    private String validateTicket(Level5TicketPayload payload) {
        if (!PredefinedLevel5BusinessSync.METRO_SINGLE_RIDE.equals(payload.getTicketType())) {
            return "INVALID_TICKET_TYPE";
        }
        if (!PredefinedLevel5BusinessSync.ROUTE_SCOPE_TYPES.contains(payload.getRouteScopeType())) {
            return "INVALID_ROUTE_SCOPE_TYPE";
        }
        if (!isValidTicketTransportType(payload.getTransportType())) {
            return "INVALID_TRANSPORT_TYPE";
        }
        if (!PredefinedLevel5BusinessSync.TICKET_USAGE_STATUSES.contains(payload.getUsageStatus())) {
            return "INVALID_TICKET_USAGE_STATUS";
        }
        if (payload.getValidFrom() == null || payload.getValidTo() == null
                || !payload.getValidTo().isAfter(payload.getValidFrom())) {
            return "INVALID_VALIDITY_PERIOD";
        }
        if (PredefinedLevel5BusinessSync.SINGLE_ROUTE.equals(payload.getRouteScopeType())
                && (isBlank(payload.getOperatorRef()) || isBlank(payload.getRouteRef()))) {
            return "SCOPE_REF_REQUIRED";
        }
        return null;
    }

    private String validateEntitlement(Level5EntitlementPayload payload) {
        if (!PredefinedLevel5BusinessSync.MONTHLY_PASS.equals(payload.getFareProductCode())) {
            return "INVALID_FARE_PRODUCT_CODE";
        }
        if (!PredefinedLevel5BusinessSync.MONTH.equals(payload.getPassPeriod())) {
            return "INVALID_PASS_PERIOD";
        }
        if (!PredefinedLevel5BusinessSync.ENTITLEMENT_SCOPES.contains(payload.getPassScope())) {
            return "INVALID_PASS_SCOPE";
        }
        if (!PredefinedLevel5BusinessSync.ENTITLEMENT_STATUSES.contains(payload.getStatus())) {
            return "INVALID_ENTITLEMENT_STATUS";
        }
        if (isBlank(payload.getOperatorRef()) || isBlank(payload.getRouteRef())) {
            return "SCOPE_REF_REQUIRED";
        }
        if (!isValidEntitlementTransportType(payload.getTransportType())) {
            return "INVALID_TRANSPORT_TYPE";
        }
        if (payload.getValidFrom() == null || payload.getValidTo() == null
                || !payload.getValidTo().isAfter(payload.getValidFrom())) {
            return "INVALID_VALIDITY_PERIOD";
        }
        return null;
    }

    private boolean isValidEntitlementTransportType(String transportType) {
        return PredefinedTransportType.BUS.equals(transportType)
                || PredefinedTransportType.METRO.equals(transportType)
                || PredefinedLevel5BusinessSync.ALL.equals(transportType);
    }

    private boolean isValidTicketTransportType(String transportType) {
        return PredefinedTransportType.BUS.equals(transportType)
                || PredefinedTransportType.METRO.equals(transportType)
                || PredefinedLevel5BusinessSync.ALL.equals(transportType);
    }

    private void createCardPlaceholderIfMissing(UUID cardId) {
        String normalizedCardId = cardId.toString();
        if (cardRepository.existsById(normalizedCardId)) {
            return;
        }
        Card card = Card.builder()
                .id(normalizedCardId)
                .cardType(PredefinedLevel5BusinessSync.PHYSICAL)
                .status(PredefinedLevel5BusinessSync.ACTIVE)
                .sourceVersion(0L)
                .syncedAt(LocalDateTime.now())
                .build();
        cardRepository.save(card);
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

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
    }

    private Long toSourceVersion(Instant instant) {
        return instant == null ? System.currentTimeMillis() : instant.toEpochMilli();
    }

    private void normalizeTicketPayload(Level5TicketPayload payload) {
        payload.setTicketType(normalizeUppercase(payload.getTicketType()));
        payload.setRouteScopeType(normalizeUppercase(payload.getRouteScopeType()));
        payload.setOperatorRef(normalize(payload.getOperatorRef()));
        payload.setRouteRef(normalize(payload.getRouteRef()));
        payload.setTransportType(normalizeUppercase(payload.getTransportType()));
        payload.setUsageStatus(normalizeUppercase(payload.getUsageStatus()));
    }

    private void normalizeEntitlementPayload(Level5EntitlementPayload payload) {
        payload.setFareProductCode(normalizeUppercase(payload.getFareProductCode()));
        payload.setPassPeriod(normalizeUppercase(payload.getPassPeriod()));
        payload.setPassScope(normalizeUppercase(payload.getPassScope()));
        payload.setOperatorRef(normalize(payload.getOperatorRef()));
        payload.setRouteRef(normalize(payload.getRouteRef()));
        payload.setTransportType(normalizeUppercase(payload.getTransportType()));
        payload.setPassengerType(normalize(payload.getPassengerType()));
        payload.setStatus(normalizeUppercase(payload.getStatus()));
    }

    private VersionDecision decideVersion(Optional<Long> currentVersion, Long incomingVersion) {
        if (currentVersion.isEmpty()) {
            return new VersionDecision(PredefinedLevel5BusinessSync.CREATED);
        }
        if (Objects.equals(currentVersion.get(), incomingVersion)) {
            return new VersionDecision(PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION);
        }
        if (currentVersion.get() > incomingVersion) {
            return new VersionDecision(PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION);
        }
        return new VersionDecision(PredefinedLevel5BusinessSync.UPDATED);
    }

    private boolean externalIdMatches(String externalId, String payloadId) {
        String normalizedExternalId = normalize(externalId);
        return normalizedExternalId == null || normalizedExternalId.equals(payloadId);
    }

    private Level5BusinessSyncResult buildResult(String syncType, String correlationId,
                                                 List<Level5BusinessSyncItemResult> items) {
        return Level5BusinessSyncResult.builder()
                .syncType(syncType)
                .correlationId(correlationId)
                .processedCount(items.size())
                .createdCount(countByResult(items, PredefinedLevel5BusinessSync.CREATED))
                .updatedCount(countByResult(items, PredefinedLevel5BusinessSync.UPDATED))
                .ignoredCount(countByResult(items, PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION)
                        + countByResult(items, PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION))
                .rejectedCount(countByResult(items, PredefinedLevel5BusinessSync.REJECTED))
                .items(items)
                .build();
    }

    private int countByResult(List<Level5BusinessSyncItemResult> items, String result) {
        return (int) items.stream().filter(item -> result.equals(item.getResult())).count();
    }

    private Level5BusinessSyncItemResult success(String externalId, String result, Long currentVersion) {
        return Level5BusinessSyncItemResult.builder()
                .externalId(externalId)
                .result(result)
                .currentVersion(currentVersion)
                .build();
    }

    private Level5BusinessSyncItemResult ignored(String externalId, String result, Long currentVersion) {
        return success(externalId, result, currentVersion);
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record VersionDecision(String result) {
        boolean isIgnored() {
            return PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION.equals(result)
                    || PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION.equals(result);
        }
    }
}
