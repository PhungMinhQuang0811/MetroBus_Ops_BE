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
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5TicketPayload;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncResult;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.integration.level5.mapper.Level5BusinessSyncMapper;
import com.vdt.afc_ops_service.repository.CardRepository;
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
        if (message == null || message.getCardId() == null) return rejected(null, null, "INVALID_CARD_STATUS_MESSAGE", "cardId is required");
        String cardId = message.getCardId().toString();
        Long sourceVersion = toSourceVersion(message.getOccurredAt());
        Level5CardPayload payload = Level5CardPayload.builder().cardId(cardId).cardUid(normalize(message.getCardUid()))
                .cardType(PredefinedLevel5BusinessSync.IDENTIFIED).status(mapC5CardStatus(message.getToStatus()))
                .statusReason(normalize(message.getReason())).build();
        return processCard(Level5BusinessSyncItemMessage.builder().externalId(cardId).sourceVersion(sourceVersion).card(payload).build());
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processC5Blacklist(String routingKey, C5BlacklistMessage message) {
        if (message == null || message.getCardId() == null) return rejected(null, null, "INVALID_BLACKLIST_MESSAGE", "cardId is required");
        String cardId = message.getCardId().toString();
        boolean removed = "blacklist.removed".equals(routingKey) || "REMOVED".equals(normalizeUppercase(message.getAction()));
        Long sourceVersion = toSourceVersion(message.getOccurredAt());
        Level5CardPayload payload = Level5CardPayload.builder().cardId(cardId).cardType(PredefinedLevel5BusinessSync.IDENTIFIED)
                .status(removed ? PredefinedLevel5BusinessSync.ACTIVE : PredefinedLevel5BusinessSync.BLACKLISTED)
                .statusReason(removed ? null : normalize(message.getReason())).build();
        return processCard(Level5BusinessSyncItemMessage.builder().externalId(cardId).sourceVersion(sourceVersion).card(payload).build());
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processC5Ticket(C5TicketMessage message) {
        if (message == null || message.getTicketId() == null) return rejected(null, null, "INVALID_TICKET_MESSAGE", "ticketId is required");
        if (message.getCardId() != null) createCardPlaceholderIfMissing(message.getCardId());
        String ticketType = normalizeUppercase(message.getType());
        if ("SINGLE_TRIP".equals(ticketType)) return processC5SingleTripTicket(message);
        if (PredefinedLevel5BusinessSync.MONTHLY_PASS.equals(ticketType)) return processC5MonthlyPass(message);
        return rejected(message.getTicketId().toString(), toSourceVersion(message.getIssuedAt()), "INVALID_TICKET_TYPE", "Unsupported C5 ticket type");
    }

    @Override
    @Transactional
    public Level5BusinessSyncItemResult processC5TicketUnlinked(C5TicketUnlinkedMessage message) {
        if (message == null || message.getTicketId() == null) return rejected(null, null, "INVALID_TICKET_UNLINKED_MESSAGE", "ticketId is required");
        String ticketId = message.getTicketId().toString();
        Optional<Ticket> ticket = ticketRepository.findById(ticketId);
        if (ticket.isEmpty()) return rejected(ticketId, null, "PRODUCT_NOT_SYNCED", "Ticket has not been synced");
        Ticket t = ticket.get();
        t.setUsageStatus(PredefinedLevel5BusinessSync.CANCELLED);
        t.setSourceVersion(System.currentTimeMillis()); t.setSyncedAt(LocalDateTime.now()); ticketRepository.save(t);
        return success(ticketId, PredefinedLevel5BusinessSync.UPDATED, t.getSourceVersion());
    }

    private Level5BusinessSyncItemResult processItem(String syncType, Level5BusinessSyncItemMessage item) {
        if (item == null) return rejected(null, null, "INVALID_ITEM", "Sync item must not be null");
        if (item.getSourceVersion() == null || item.getSourceVersion() < 0) return rejected(item.getExternalId(), null, "INVALID_SOURCE_VERSION", "sourceVersion must be non-negative");
        return switch (syncType) {
            case PredefinedLevel5BusinessSync.CARD_UPSERT, PredefinedLevel5BusinessSync.CARD_STATUS_CHANGED -> processCard(item);
            default -> processTicket(item);
        };
    }

    private Level5BusinessSyncItemResult processCard(Level5BusinessSyncItemMessage item) {
        Level5CardPayload payload = item.getCard();
        if (payload == null) return rejected(item.getExternalId(), item.getSourceVersion(), "CARD_PAYLOAD_REQUIRED", "card payload required");
        String cardId = normalize(payload.getCardId());
        if (isBlank(cardId) || !externalIdMatches(item.getExternalId(), cardId)) return rejected(item.getExternalId(), item.getSourceVersion(), "INVALID_CARD_ID", "cardId is required");
        payload.setCardId(cardId); payload.setCardType(normalizeUppercase(payload.getCardType())); payload.setStatus(normalizeUppercase(payload.getStatus()));
        String ve = validateCard(payload);
        if (ve != null) return rejected(cardId, item.getSourceVersion(), ve, "Invalid card payload");
        Optional<Card> ec = cardRepository.findById(cardId);
        VersionDecision vd = decideVersion(ec.map(Card::getSourceVersion), item.getSourceVersion());
        if (vd.isIgnored()) return ignored(cardId, vd.result(), ec.map(Card::getSourceVersion).orElse(null));
        LocalDateTime syncedAt = LocalDateTime.now();
        Card card = ec.orElseGet(() -> mapper.toCard(payload, item.getSourceVersion(), syncedAt));
        if (ec.isPresent()) mapper.updateCard(card, payload, item.getSourceVersion(), syncedAt);
        cardRepository.save(card);
        return success(cardId, vd.result(), item.getSourceVersion());
    }

    private Level5BusinessSyncItemResult processTicket(Level5BusinessSyncItemMessage item) {
        Level5TicketPayload payload = item.getTicket();
        if (payload == null) return rejected(item.getExternalId(), item.getSourceVersion(), "TICKET_PAYLOAD_REQUIRED", "ticket payload required");
        String ticketId = normalize(payload.getTicketId());
        String cardId = normalize(payload.getCardId());
        if (isBlank(ticketId) || !externalIdMatches(item.getExternalId(), ticketId)) return rejected(item.getExternalId(), item.getSourceVersion(), "INVALID_TICKET_ID", "ticketId is required");
        payload.setTicketId(ticketId); payload.setCardId(cardId);
        payload.setTicketType(normalizeUppercase(payload.getTicketType())); payload.setScope(normalizeUppercase(payload.getScope()));
        payload.setMode(normalizeUppercase(payload.getMode())); payload.setUsageStatus(normalizeUppercase(payload.getUsageStatus()));
        Optional<Ticket> et = ticketRepository.findById(ticketId);
        VersionDecision vd = decideVersion(et.map(Ticket::getSourceVersion), item.getSourceVersion());
        if (vd.isIgnored()) return ignored(ticketId, vd.result(), et.map(Ticket::getSourceVersion).orElse(null));
        LocalDateTime syncedAt = LocalDateTime.now();
        Ticket ticket = et.orElseGet(() -> mapper.toTicket(payload, null, item.getSourceVersion(), syncedAt));
        if (et.isPresent()) mapper.updateTicket(ticket, payload, null, item.getSourceVersion(), syncedAt);
        if (cardId != null && ticket.getCard() == null) cardRepository.findById(cardId).ifPresent(ticket::setCard);
        ticketRepository.save(ticket);
        return success(ticketId, vd.result(), item.getSourceVersion());
    }

    private Level5BusinessSyncItemResult processC5SingleTripTicket(C5TicketMessage message) {
        Level5TicketPayload payload = Level5TicketPayload.builder()
                .ticketId(message.getTicketId().toString()).cardId(message.getCardId() == null ? null : message.getCardId().toString())
                .ticketType(PredefinedLevel5BusinessSync.METRO_SINGLE_RIDE)
                .scope(PredefinedLevel5BusinessSync.NETWORK)
                .mode(normalizeUppercase(message.getMode()) != null ? normalizeUppercase(message.getMode()) : PredefinedTransportType.METRO)
                .operatorRef("*").routeRef("*")
                .usageStatus(mapC5TicketStatus(message.getStatus()))
                .validFrom(toStartOfDay(message.getValidFrom())).validTo(toStartOfDay(message.getValidTo())).usedAt(toLocalDateTime(message.getUsedAt()))
                .build();
        return processTicket(Level5BusinessSyncItemMessage.builder().externalId(message.getTicketId().toString()).sourceVersion(toSourceVersion(message.getIssuedAt())).ticket(payload).build());
    }

    private Level5BusinessSyncItemResult processC5MonthlyPass(C5TicketMessage message) {
        Level5TicketPayload payload = Level5TicketPayload.builder()
                .ticketId(message.getTicketId().toString()).cardId(message.getCardId() == null ? null : message.getCardId().toString())
                .ticketType(PredefinedLevel5BusinessSync.MONTHLY_PASS)
                .scope(mapC5PassScope(message.getScope()))
                .mode(normalizeUppercase(message.getMode()) != null ? normalizeUppercase(message.getMode()) : PredefinedTransportType.METRO)
                .operatorRef("*").routeRef("*")
                .usageStatus(mapC5EntitlementStatus(message.getStatus()))
                .validFrom(toStartOfDay(message.getValidFrom())).validTo(toStartOfDay(message.getValidTo()))
                .build();
        return processTicket(Level5BusinessSyncItemMessage.builder().externalId(message.getTicketId().toString()).sourceVersion(toSourceVersion(message.getIssuedAt())).ticket(payload).build());
    }

    private String validateCard(Level5CardPayload payload) {
        if (!PredefinedLevel5BusinessSync.CARD_TYPES.contains(payload.getCardType())) return "INVALID_CARD_TYPE";
        if (!PredefinedLevel5BusinessSync.CARD_STATUSES.contains(payload.getStatus())) return "INVALID_CARD_STATUS";
        return null;
    }

    private void createCardPlaceholderIfMissing(UUID cardId) {
        String id = cardId.toString(); if (cardRepository.existsById(id)) return;
        cardRepository.save(Card.builder().id(id).cardType(PredefinedLevel5BusinessSync.IDENTIFIED).status(PredefinedLevel5BusinessSync.ACTIVE).sourceVersion(0L).syncedAt(LocalDateTime.now()).build());
    }

    private String mapC5CardStatus(String s) { s = normalizeUppercase(s); if (s == null) return PredefinedLevel5BusinessSync.INACTIVE; return switch (s) { case "ACTIVE" -> PredefinedLevel5BusinessSync.ACTIVE; case "REVOKED", "DISPOSED" -> PredefinedLevel5BusinessSync.CANCELLED; default -> PredefinedLevel5BusinessSync.INACTIVE; }; }
    private String mapC5TicketStatus(String s) { s = normalizeUppercase(s); if (s == null) return PredefinedLevel5BusinessSync.UNUSED; return switch (s) { case "USED" -> PredefinedLevel5BusinessSync.USED; case "EXPIRED" -> PredefinedLevel5BusinessSync.EXPIRED; case "CANCELLED", "REVOKED" -> PredefinedLevel5BusinessSync.CANCELLED; case "ACTIVE" -> PredefinedLevel5BusinessSync.ACTIVE; default -> PredefinedLevel5BusinessSync.UNUSED; }; }
    private String mapC5EntitlementStatus(String s) { s = normalizeUppercase(s); if (s == null) return PredefinedLevel5BusinessSync.ACTIVE; return switch (s) { case "EXPIRED" -> PredefinedLevel5BusinessSync.EXPIRED; case "CANCELLED", "REVOKED" -> PredefinedLevel5BusinessSync.CANCELLED; default -> PredefinedLevel5BusinessSync.ACTIVE; }; }
    private String mapC5PassScope(String s) { s = normalizeUppercase(s); if (s == null) return PredefinedLevel5BusinessSync.NETWORK; return "MULTI_ROUTE".equals(s) ? PredefinedLevel5BusinessSync.INTERLINE : PredefinedLevel5BusinessSync.SINGLE_ROUTE; }
    private String mapC5TransportType(String mode) { mode = normalizeUppercase(mode); if (mode == null) return PredefinedTransportType.METRO; return switch (mode) { case "BUS" -> PredefinedTransportType.BUS; case "ANY" -> PredefinedLevel5BusinessSync.ALL; default -> PredefinedTransportType.METRO; }; }
    private LocalDateTime toStartOfDay(LocalDate d) { return d == null ? null : d.atStartOfDay(); }
    private LocalDateTime toLocalDateTime(Instant i) { return i == null ? null : LocalDateTime.ofInstant(i, java.time.ZoneId.systemDefault()); }
    private Long toSourceVersion(Instant i) { return i == null ? System.currentTimeMillis() : i.toEpochMilli(); }

    private record VersionDecision(String result) { boolean isIgnored() { return PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION.equals(result) || PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION.equals(result); } }
    private VersionDecision decideVersion(Optional<Long> cv, Long iv) { if (cv.isEmpty()) return new VersionDecision(PredefinedLevel5BusinessSync.CREATED); if (Objects.equals(cv.get(), iv)) return new VersionDecision(PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION); if (cv.get() > iv) return new VersionDecision(PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION); return new VersionDecision(PredefinedLevel5BusinessSync.UPDATED); }
    private boolean externalIdMatches(String extId, String id) { return Objects.equals(extId, id); }
    private boolean isBlank(String s) { return s == null || s.isBlank(); }
    private String normalizeUppercase(String v) { String n = normalize(v); return n == null ? null : n.toUpperCase(); }
    private String normalize(String v) { if (v == null) return null; String n = v.trim(); return n.isEmpty() ? null : n; }

    private Level5BusinessSyncItemResult success(String id, String r, Long cv) { return Level5BusinessSyncItemResult.builder().externalId(id).result(r).currentVersion(cv).build(); }
    private Level5BusinessSyncItemResult ignored(String id, String r, Long cv) { return Level5BusinessSyncItemResult.builder().externalId(id).result(r).currentVersion(cv).message("Ignored").build(); }
    private Level5BusinessSyncItemResult rejected(String id, Long cv, String ec, String msg) { return Level5BusinessSyncItemResult.builder().externalId(id).result(PredefinedLevel5BusinessSync.REJECTED).currentVersion(cv).errorCode(ec).message(msg).build(); }
    private Level5BusinessSyncResult buildResult(String st, String ci, List<Level5BusinessSyncItemResult> items) { long cr = items.stream().filter(i -> PredefinedLevel5BusinessSync.CREATED.equals(i.getResult())).count(); long up = items.stream().filter(i -> PredefinedLevel5BusinessSync.UPDATED.equals(i.getResult())).count(); long ig = items.stream().filter(i -> PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION.equals(i.getResult()) || PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION.equals(i.getResult())).count(); long rj = items.stream().filter(i -> PredefinedLevel5BusinessSync.REJECTED.equals(i.getResult())).count(); return Level5BusinessSyncResult.builder().syncType(st).correlationId(ci).processedCount(items.size()).createdCount(cr).updatedCount(up).ignoredCount(ig).rejectedCount(rj).items(items).build(); }
}