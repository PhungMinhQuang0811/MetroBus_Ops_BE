package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5BusinessSyncItemMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5BusinessSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5CardPayload;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5TicketPayload;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5BlacklistMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardStatusMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncResult;
import com.vdt.afc_ops_service.integration.level5.mapper.Level5BusinessSyncMapper;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Level5BusinessSyncServiceTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    TicketRepository ticketRepository;

    Level5BusinessSyncMapper mapper = new Level5BusinessSyncMapper();

    Level5BusinessSyncService service;

    @BeforeEach
    void setUp() {
        service = new Level5BusinessSyncService(cardRepository, ticketRepository, mapper);
    }

    @Test
    void processSync_invalidSyncType_returnsRejected() {
        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("INVALID_TYPE")
                .correlationId("corr-1")
                .build();

        Level5BusinessSyncResult result = service.processSync(message);

        assertEquals("INVALID_TYPE", result.getSyncType());
        assertEquals("corr-1", result.getCorrelationId());
        assertEquals(1, result.getRejectedCount());
        assertEquals("INVALID_SYNC_TYPE", result.getItems().get(0).getErrorCode());
    }

    @Test
    void processSync_nullOrEmptyItems_returnsRejected() {
        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(null)
                .build();

        Level5BusinessSyncResult result1 = service.processSync(message);
        assertEquals(1, result1.getRejectedCount());
        assertEquals("EMPTY_ITEMS", result1.getItems().get(0).getErrorCode());

        Level5BusinessSyncMessage message2 = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of())
                .build();

        Level5BusinessSyncResult result2 = service.processSync(message2);
        assertEquals(1, result2.getRejectedCount());
    }

    @Test
    void processSync_invalidSourceVersion_returnsRejected() {
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("CARD-001")
                .sourceVersion(-1L)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of(item))
                .build();

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals(1, result.getRejectedCount());
        assertEquals("INVALID_SOURCE_VERSION", result.getItems().get(0).getErrorCode());
    }

    @Test
    void processSync_nullCardPayload_returnsRejected() {
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("CARD-001")
                .sourceVersion(100L)
                .card(null)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of(item))
                .build();

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals("CARD_PAYLOAD_REQUIRED", result.getItems().get(0).getErrorCode());
    }

    @Test
    void processSync_emptyCardId_returnsRejected() {
        Level5CardPayload payload = Level5CardPayload.builder().cardId(" ").build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("CARD-001")
                .sourceVersion(100L)
                .card(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of(item))
                .build();

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals("INVALID_CARD_ID", result.getItems().get(0).getErrorCode());
    }

    @Test
    void processSync_invalidCardType_returnsRejected() {
        Level5CardPayload payload = Level5CardPayload.builder().cardId("CARD-001").cardType("UNKNOWN").status("ACTIVE").build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("CARD-001")
                .sourceVersion(100L)
                .card(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of(item))
                .build();

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals("INVALID_CARD_TYPE", result.getItems().get(0).getErrorCode());
    }

    @Test
    void processSync_invalidCardStatus_returnsRejected() {
        Level5CardPayload payload = Level5CardPayload.builder().cardId("CARD-001").cardType("IDENTIFIED").status("DELETED").build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("CARD-001")
                .sourceVersion(100L)
                .card(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of(item))
                .build();

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals("INVALID_CARD_STATUS", result.getItems().get(0).getErrorCode());
    }

    @Test
    void processSync_cardCreated_returnsCreated() {
        Level5CardPayload payload = Level5CardPayload.builder().cardId("CARD-001").cardType("IDENTIFIED").status("ACTIVE").build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("CARD-001")
                .sourceVersion(100L)
                .card(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of(item))
                .build();

        when(cardRepository.findById("CARD-001")).thenReturn(Optional.empty());

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals(1, result.getCreatedCount());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void processSync_cardSameVersion_returnsIgnored() {
        Level5CardPayload payload = Level5CardPayload.builder().cardId("CARD-001").cardType("IDENTIFIED").status("ACTIVE").build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("CARD-001")
                .sourceVersion(100L)
                .card(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of(item))
                .build();

        Card existingCard = Card.builder().id("CARD-001").sourceVersion(100L).build();
        when(cardRepository.findById("CARD-001")).thenReturn(Optional.of(existingCard));

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals(1, result.getIgnoredCount());
        verify(cardRepository, never()).save(any());
    }

    @Test
    void processSync_cardStaleVersion_returnsIgnored() {
        Level5CardPayload payload = Level5CardPayload.builder().cardId("CARD-001").cardType("IDENTIFIED").status("ACTIVE").build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("CARD-001")
                .sourceVersion(90L)
                .card(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of(item))
                .build();

        Card existingCard = Card.builder().id("CARD-001").sourceVersion(100L).build();
        when(cardRepository.findById("CARD-001")).thenReturn(Optional.of(existingCard));

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals(1, result.getIgnoredCount());
        assertEquals(PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION, result.getItems().get(0).getResult());
    }

    @Test
    void processSync_cardUpdated_returnsUpdated() {
        Level5CardPayload payload = Level5CardPayload.builder().cardId("CARD-001").cardType("IDENTIFIED").status("ACTIVE").build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("CARD-001")
                .sourceVersion(110L)
                .card(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("CARD_UPSERT")
                .items(List.of(item))
                .build();

        Card existingCard = Card.builder().id("CARD-001").sourceVersion(100L).build();
        when(cardRepository.findById("CARD-001")).thenReturn(Optional.of(existingCard));

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals(1, result.getUpdatedCount());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void processSync_nullTicketPayload_returnsRejected() {
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("TICKET-001")
                .sourceVersion(100L)
                .ticket(null)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("TICKET_UPSERT")
                .items(List.of(item))
                .build();

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals("TICKET_PAYLOAD_REQUIRED", result.getItems().get(0).getErrorCode());
    }

    @Test
    void processSync_emptyTicketId_returnsRejected() {
        Level5TicketPayload payload = Level5TicketPayload.builder().ticketId("").build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("TICKET-001")
                .sourceVersion(100L)
                .ticket(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("TICKET_UPSERT")
                .items(List.of(item))
                .build();

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals("INVALID_TICKET_ID", result.getItems().get(0).getErrorCode());
    }

    @Test
    void processSync_ticketCreated_linksCardIfSpecified() {
        Level5TicketPayload payload = Level5TicketPayload.builder()
                .ticketId("TICKET-001")
                .cardId("CARD-001")
                .ticketType("SINGLE_TRIP")
                .usageStatus("UNUSED")
                .build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("TICKET-001")
                .sourceVersion(100L)
                .ticket(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("TICKET_UPSERT")
                .items(List.of(item))
                .build();

        when(ticketRepository.findById("TICKET-001")).thenReturn(Optional.empty());
        Card card = Card.builder().id("CARD-001").build();
        when(cardRepository.findById("CARD-001")).thenReturn(Optional.of(card));

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals(1, result.getCreatedCount());
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> card.equals(t.getCard())));
    }

    @Test
    void processSync_ticketSameVersion_returnsIgnored() {
        Level5TicketPayload payload = Level5TicketPayload.builder()
                .ticketId("TICKET-001")
                .build();
        Level5BusinessSyncItemMessage item = Level5BusinessSyncItemMessage.builder()
                .externalId("TICKET-001")
                .sourceVersion(100L)
                .ticket(payload)
                .build();

        Level5BusinessSyncMessage message = Level5BusinessSyncMessage.builder()
                .syncType("TICKET_UPSERT")
                .items(List.of(item))
                .build();

        Ticket existingTicket = Ticket.builder().id("TICKET-001").sourceVersion(100L).build();
        when(ticketRepository.findById("TICKET-001")).thenReturn(Optional.of(existingTicket));

        Level5BusinessSyncResult result = service.processSync(message);
        assertEquals(1, result.getIgnoredCount());
    }

    @Test
    void processC5CardStatus_nullMessageOrNullCardId_returnsRejected() {
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processC5CardStatus(null).getResult());

        C5CardStatusMessage msg = new C5CardStatusMessage();
        msg.setCardId(null);
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processC5CardStatus(msg).getResult());
    }

    @Test
    void processC5CardStatus_validMessage_upsertsCard() {
        UUID cardId = UUID.randomUUID();
        C5CardStatusMessage msg = new C5CardStatusMessage();
        msg.setCardId(cardId);
        msg.setCardUid("UID1");
        msg.setToStatus("ACTIVE");
        msg.setReason("Test");
        msg.setOccurredAt(Instant.now());

        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processC5CardStatus(msg);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        assertEquals(cardId.toString(), result.getExternalId());
    }

    @Test
    void processC5Blacklist_nullMessageOrNullCardId_returnsRejected() {
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processC5Blacklist("blacklist.added", null).getResult());

        C5BlacklistMessage msg = new C5BlacklistMessage(UUID.randomUUID(), null, "ADDED", "LOST", UUID.randomUUID(), Instant.now());
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processC5Blacklist("blacklist.added", msg).getResult());
    }

    @Test
    void processC5Blacklist_removedAction_marksActive() {
        UUID cardId = UUID.randomUUID();
        C5BlacklistMessage msg = new C5BlacklistMessage(UUID.randomUUID(), cardId, "REMOVED", null, UUID.randomUUID(), Instant.now());

        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processC5Blacklist("blacklist.removed", msg);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        verify(cardRepository).save(org.mockito.Mockito.argThat(c -> "ACTIVE".equals(c.getStatus())));
    }

    @Test
    void processC5Blacklist_addedAction_marksBlacklisted() {
        UUID cardId = UUID.randomUUID();
        C5BlacklistMessage msg = new C5BlacklistMessage(UUID.randomUUID(), cardId, "ADDED", "FRAUD", UUID.randomUUID(), Instant.now());

        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processC5Blacklist("blacklist.added", msg);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        verify(cardRepository).save(org.mockito.Mockito.argThat(c -> "BLACKLISTED".equals(c.getStatus())));
    }

    @Test
    void processC5Ticket_nullMessageOrNullTicketId_returnsRejected() {
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processC5Ticket(null).getResult());

        C5TicketMessage msg = C5TicketMessage.builder().ticketId(null).build();
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processC5Ticket(msg).getResult());
    }

    @Test
    void processC5Ticket_unsupportedTicketType_returnsRejected() {
        UUID ticketId = UUID.randomUUID();
        C5TicketMessage msg = C5TicketMessage.builder()
                .ticketId(ticketId)
                .type("WEEKLY_PASS")
                .build();

        Level5BusinessSyncItemResult result = service.processC5Ticket(msg);
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, result.getResult());
        assertEquals("INVALID_TICKET_TYPE", result.getErrorCode());
    }

    @Test
    void processC5Ticket_singleTrip_createsCardPlaceholderIfMissing() {
        UUID ticketId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        C5TicketMessage msg = C5TicketMessage.builder()
                .ticketId(ticketId)
                .cardId(cardId)
                .type("SINGLE_TRIP")
                .mode("METRO")
                .status("USED")
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now())
                .usedAt(Instant.now())
                .issuedAt(Instant.now())
                .fareAmount(BigDecimal.valueOf(10))
                .build();

        when(cardRepository.existsById(cardId.toString())).thenReturn(false);
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processC5Ticket(msg);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        // Verify placeholder card is created
        verify(cardRepository).save(org.mockito.Mockito.argThat(c -> cardId.toString().equals(c.getId())));
        // Verify ticket is saved
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void processC5Ticket_monthlyPass_upsertsTicket() {
        UUID ticketId = UUID.randomUUID();
        C5TicketMessage msg = C5TicketMessage.builder()
                .ticketId(ticketId)
                .type("MONTHLY_PASS")
                .mode("BUS")
                .scope("MULTI_ROUTE")
                .status("ACTIVE")
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now())
                .issuedAt(Instant.now())
                .build();

        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processC5Ticket(msg);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> "MONTHLY_PASS".equals(t.getType())));
    }

    @Test
    void processC5TicketUnlinked_nullMessageOrNullTicketId_returnsRejected() {
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processC5TicketUnlinked(null).getResult());

        C5TicketUnlinkedMessage msg = new C5TicketUnlinkedMessage();
        msg.setTicketId(null);
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processC5TicketUnlinked(msg).getResult());
    }

    @Test
    void processC5TicketUnlinked_ticketNotSynced_returnsRejected() {
        UUID ticketId = UUID.randomUUID();
        C5TicketUnlinkedMessage msg = new C5TicketUnlinkedMessage();
        msg.setTicketId(ticketId);

        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processC5TicketUnlinked(msg);
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, result.getResult());
        assertEquals("PRODUCT_NOT_SYNCED", result.getErrorCode());
    }

    @Test
    void processC5TicketUnlinked_ticketSynced_cancelsTicket() {
        UUID ticketId = UUID.randomUUID();
        C5TicketUnlinkedMessage msg = new C5TicketUnlinkedMessage();
        msg.setTicketId(ticketId);

        Ticket ticket = Ticket.builder().id(ticketId.toString()).usageStatus("UNUSED").build();
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.of(ticket));

        Level5BusinessSyncItemResult result = service.processC5TicketUnlinked(msg);
        assertEquals(PredefinedLevel5BusinessSync.UPDATED, result.getResult());
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> "CANCELLED".equals(t.getUsageStatus())));
    }

    @Test
    void processC5CardStatus_statusMappingVariantsAndNullEventTime() {
        UUID cardId = UUID.randomUUID();
        C5CardStatusMessage msg1 = new C5CardStatusMessage();
        msg1.setCardId(cardId);
        msg1.setToStatus("REVOKED");
        msg1.setOccurredAt(null);
        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.empty());
        service.processC5CardStatus(msg1);
        verify(cardRepository).save(org.mockito.Mockito.argThat(c -> "CANCELLED".equals(c.getStatus())));

        org.mockito.Mockito.clearInvocations(cardRepository);

        UUID cardId2 = UUID.randomUUID();
        C5CardStatusMessage msg2 = new C5CardStatusMessage();
        msg2.setCardId(cardId2);
        msg2.setToStatus("INVALID_STATUS");
        msg2.setOccurredAt(null);
        when(cardRepository.findById(cardId2.toString())).thenReturn(Optional.empty());
        service.processC5CardStatus(msg2);
        verify(cardRepository).save(org.mockito.Mockito.argThat(c -> "INACTIVE".equals(c.getStatus())));

        org.mockito.Mockito.clearInvocations(cardRepository);

        UUID cardId3 = UUID.randomUUID();
        C5CardStatusMessage msg3 = new C5CardStatusMessage();
        msg3.setCardId(cardId3);
        msg3.setToStatus(null);
        msg3.setOccurredAt(null);
        when(cardRepository.findById(cardId3.toString())).thenReturn(Optional.empty());
        service.processC5CardStatus(msg3);
        verify(cardRepository).save(org.mockito.Mockito.argThat(c -> "INACTIVE".equals(c.getStatus())));
    }

    @Test
    void processC5Ticket_monthlyPass_statusMappingAndOptionalVariants() {
        // EXPIRED status, null fareAmount, null usedAt, null validFrom, null validTo, MULTI_ROUTE scope
        UUID ticketId = UUID.randomUUID();
        C5TicketMessage msgEx = C5TicketMessage.builder()
                .ticketId(ticketId)
                .type("MONTHLY_PASS")
                .status("EXPIRED")
                .scope("MULTI_ROUTE")
                .fareAmount(null)
                .usedAt(null)
                .validFrom(null)
                .validTo(null)
                .issuedAt(null)
                .build();
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());
        service.processC5Ticket(msgEx);
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> 
                "EXPIRED".equals(t.getUsageStatus()) && 
                "INTERLINE".equals(t.getScope()) &&
                t.getPrice() == null &&
                t.getUsedAt() == null &&
                t.getValidFrom() == null &&
                t.getValidTo() == null
        ));

        org.mockito.Mockito.clearInvocations(ticketRepository);

        // REVOKED status, SINGLE_ROUTE scope
        UUID ticketIdRev = UUID.randomUUID();
        C5TicketMessage msgRev = C5TicketMessage.builder()
                .ticketId(ticketIdRev)
                .type("MONTHLY_PASS")
                .status("REVOKED")
                .scope("SINGLE_ROUTE")
                .build();
        when(ticketRepository.findById(ticketIdRev.toString())).thenReturn(Optional.empty());
        service.processC5Ticket(msgRev);
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> 
                "CANCELLED".equals(t.getUsageStatus()) &&
                "SINGLE_ROUTE".equals(t.getScope())
        ));

        org.mockito.Mockito.clearInvocations(ticketRepository);

        // CANCELLED status, null scope
        UUID ticketIdCan = UUID.randomUUID();
        C5TicketMessage msgCan = C5TicketMessage.builder()
                .ticketId(ticketIdCan)
                .type("MONTHLY_PASS")
                .status("CANCELLED")
                .scope(null)
                .build();
        when(ticketRepository.findById(ticketIdCan.toString())).thenReturn(Optional.empty());
        service.processC5Ticket(msgCan);
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> 
                "CANCELLED".equals(t.getUsageStatus()) &&
                "NETWORK".equals(t.getScope())
        ));

        org.mockito.Mockito.clearInvocations(ticketRepository);

        // ACTIVE status
        UUID ticketIdAct = UUID.randomUUID();
        C5TicketMessage msgAct = C5TicketMessage.builder()
                .ticketId(ticketIdAct)
                .type("MONTHLY_PASS")
                .status("ACTIVE")
                .build();
        when(ticketRepository.findById(ticketIdAct.toString())).thenReturn(Optional.empty());
        service.processC5Ticket(msgAct);
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> "ACTIVE".equals(t.getUsageStatus())));

        org.mockito.Mockito.clearInvocations(ticketRepository);

        // INVALID status
        UUID ticketIdInv = UUID.randomUUID();
        C5TicketMessage msgInv = C5TicketMessage.builder()
                .ticketId(ticketIdInv)
                .type("MONTHLY_PASS")
                .status("INVALID_STATUS")
                .build();
        when(ticketRepository.findById(ticketIdInv.toString())).thenReturn(Optional.empty());
        service.processC5Ticket(msgInv);
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> "ACTIVE".equals(t.getUsageStatus())));

        org.mockito.Mockito.clearInvocations(ticketRepository);

        // null status
        UUID ticketIdNull = UUID.randomUUID();
        C5TicketMessage msgNull = C5TicketMessage.builder()
                .ticketId(ticketIdNull)
                .type("MONTHLY_PASS")
                .status(null)
                .build();
        when(ticketRepository.findById(ticketIdNull.toString())).thenReturn(Optional.empty());
        service.processC5Ticket(msgNull);
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> "ACTIVE".equals(t.getUsageStatus())));
    }

    @Test
    void createCardPlaceholderIfMissing_existingCard_doesNotSave() {
        UUID ticketId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        C5TicketMessage msg = C5TicketMessage.builder()
                .ticketId(ticketId)
                .cardId(cardId)
                .type("SINGLE_TRIP")
                .status("USED")
                .build();

        when(cardRepository.existsById(cardId.toString())).thenReturn(true);
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());

        service.processC5Ticket(msg);

        // Verify that cardRepository.save is never called
        verify(cardRepository, never()).save(any(Card.class));
    }
}
