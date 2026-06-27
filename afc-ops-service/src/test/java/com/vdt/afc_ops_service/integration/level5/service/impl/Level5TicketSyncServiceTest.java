package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Level5TicketSyncServiceTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    TicketRepository ticketRepository;

    Level5TicketSyncService service;

    @BeforeEach
    void setUp() {
        service = new Level5TicketSyncService(cardRepository, ticketRepository);
    }

    @Test
    void processTicket_nullMessageOrNullTicketId_returnsRejected() {
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processTicket(null).getResult());

        C5TicketMessage msg = C5TicketMessage.builder().ticketId(null).build();
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processTicket(msg).getResult());
    }

    @Test
    void processTicket_unsupportedTicketType_returnsIgnored() {
        UUID ticketId = UUID.randomUUID();
        C5TicketMessage msg = C5TicketMessage.builder()
                .ticketId(ticketId)
                .type("HOURLY_PASS")
                .build();

        Level5BusinessSyncItemResult result = service.processTicket(msg);
        assertEquals(PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION, result.getResult());
    }

    @Test
    void processTicket_ticketExistsWithNewerVersion_returnsIgnored() {
        UUID ticketId = UUID.randomUUID();
        C5TicketMessage msg = C5TicketMessage.builder()
                .ticketId(ticketId)
                .type("SINGLE_TRIP")
                .issuedAt(Instant.ofEpochMilli(100L))
                .build();

        Ticket existing = Ticket.builder().id(ticketId.toString()).sourceVersion(200L).build();
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.of(existing));

        Level5BusinessSyncItemResult result = service.processTicket(msg);
        assertEquals(PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION, result.getResult());
        assertEquals("Ticket version ignored", result.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void processTicket_createsPlaceholderCard_whenMissing() {
        UUID ticketId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        C5TicketMessage msg = C5TicketMessage.builder()
                .ticketId(ticketId)
                .cardId(cardId)
                .type("SINGLE_TRIP")
                .status("USED")
                .issuedAt(Instant.now())
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now())
                .usedAt(Instant.now())
                .fareAmount(BigDecimal.TEN)
                .build();

        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processTicket(msg);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        verify(cardRepository).save(any(Card.class));
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    void processTicket_usesExistingCard_whenPresent() {
        UUID ticketId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        C5TicketMessage msg = C5TicketMessage.builder()
                .ticketId(ticketId)
                .cardId(cardId)
                .type("SINGLE_TRIP")
                .status("EXPIRED")
                .issuedAt(Instant.now())
                .build();

        Card existingCard = Card.builder().id(cardId.toString()).build();
        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.of(existingCard));
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processTicket(msg);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        verify(cardRepository, never()).save(any());
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> existingCard.equals(t.getCard())));
    }

    @Test
    void processTicket_statusMappingVariants() {
        // Test ACTIVE status mapping
        UUID ticketId = UUID.randomUUID();
        C5TicketMessage msg1 = C5TicketMessage.builder()
                .ticketId(ticketId)
                .type("SINGLE_TRIP")
                .status("ACTIVE")
                .issuedAt(Instant.now())
                .build();
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());
        service.processTicket(msg1);
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> "ACTIVE".equals(t.getUsageStatus())));

        // Test CANCELLED status mapping
        UUID ticketId2 = UUID.randomUUID();
        C5TicketMessage msg2 = C5TicketMessage.builder()
                .ticketId(ticketId2)
                .type("SINGLE_TRIP")
                .status("REVOKED")
                .issuedAt(Instant.now())
                .build();
        when(ticketRepository.findById(ticketId2.toString())).thenReturn(Optional.empty());
        service.processTicket(msg2);
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> "CANCELLED".equals(t.getUsageStatus())));
    }

    @Test
    void processTicketSnapshot_nullMessageOrNullId_returnsRejected() {
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processTicketSnapshot(null).getResult());

        C5TicketSyncMessage msg = C5TicketSyncMessage.builder().id(null).build();
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processTicketSnapshot(msg).getResult());
    }

    @Test
    void processTicketSnapshot_validMessage_upsertsTicket() {
        UUID ticketId = UUID.randomUUID();
        UUID cardId = UUID.randomUUID();
        UUID fromStationId = UUID.randomUUID();
        UUID toStationId = UUID.randomUUID();
        C5TicketSyncMessage msg = C5TicketSyncMessage.builder()
                .id(ticketId)
                .cardId(cardId)
                .type("MONTHLY_PASS")
                .mode("METRO")
                .scope("SINGLE_ROUTE")
                .fromStationId(fromStationId)
                .toStationId(toStationId)
                .price(BigDecimal.TEN)
                .status("ACTIVE")
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now())
                .usedAt(Instant.now())
                .purchasedAt(Instant.now())
                .build();

        Card existingCard = Card.builder().id(cardId.toString()).build();
        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.of(existingCard));
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processTicketSnapshot(msg);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> 
                ticketId.toString().equals(t.getId()) &&
                fromStationId.toString().equals(t.getFromStationRef()) &&
                toStationId.toString().equals(t.getToStationRef())
        ));
    }

    @Test
    void processTicketUnlinked_nullMessageOrNullTicketId_returnsRejected() {
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processTicketUnlinked(null).getResult());

        C5TicketUnlinkedMessage msg = new C5TicketUnlinkedMessage();
        msg.setTicketId(null);
        assertEquals(PredefinedLevel5BusinessSync.REJECTED, service.processTicketUnlinked(msg).getResult());
    }

    @Test
    void processTicketUnlinked_nonExistentTicket_returnsIgnored() {
        UUID ticketId = UUID.randomUUID();
        C5TicketUnlinkedMessage msg = new C5TicketUnlinkedMessage();
        msg.setTicketId(ticketId);

        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.empty());

        Level5BusinessSyncItemResult result = service.processTicketUnlinked(msg);
        assertEquals(PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION, result.getResult());
    }

    @Test
    void processTicketUnlinked_existingTicket_cancelsTicket() {
        UUID ticketId = UUID.randomUUID();
        C5TicketUnlinkedMessage msg = new C5TicketUnlinkedMessage();
        msg.setTicketId(ticketId);

        Ticket existing = Ticket.builder().id(ticketId.toString()).usageStatus("UNUSED").build();
        when(ticketRepository.findById(ticketId.toString())).thenReturn(Optional.of(existing));

        Level5BusinessSyncItemResult result = service.processTicketUnlinked(msg);
        assertEquals(PredefinedLevel5BusinessSync.UPDATED, result.getResult());
        verify(ticketRepository).save(org.mockito.Mockito.argThat(t -> "CANCELLED".equals(t.getUsageStatus())));
    }
}
