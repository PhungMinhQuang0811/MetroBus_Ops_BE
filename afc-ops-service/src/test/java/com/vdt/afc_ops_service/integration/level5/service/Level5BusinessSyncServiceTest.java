package com.vdt.afc_ops_service.integration.level5.service;

import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5BusinessSyncItemMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5BusinessSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5CardPayload;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5EntitlementPayload;
import com.vdt.afc_ops_service.integration.level5.dto.message.Level5TicketPayload;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Entitlement;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.integration.level5.mapper.Level5BusinessSyncMapper;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.EntitlementRepository;
import com.vdt.afc_ops_service.repository.TicketRepository;
import com.vdt.afc_ops_service.integration.level5.service.impl.Level5BusinessSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Mock
    EntitlementRepository entitlementRepository;

    Level5BusinessSyncService service;

    @BeforeEach
    void setUp() {
        service = new Level5BusinessSyncService(
                cardRepository,
                ticketRepository,
                entitlementRepository,
                new Level5BusinessSyncMapper()
        );
    }

    @Test
    void processSync_NewCard_CreatesCard() {
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.processSync(Level5BusinessSyncMessage.builder()
                .syncType(PredefinedLevel5BusinessSync.CARD_UPSERT)
                .correlationId("sync-001")
                .items(List.of(Level5BusinessSyncItemMessage.builder()
                        .externalId("CARD-000001")
                        .sourceVersion(1L)
                        .card(cardPayload())
                        .build()))
                .build());

        assertEquals(1, result.getCreatedCount());
        assertEquals(0, result.getRejectedCount());
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getItems().get(0).getResult());
        verify(cardRepository).save(any(Card.class));
    }

    @Test
    void processSync_ExistingCardWithNewVersion_UpdatesCard() {
        Card existing = Card.builder()
                .id("CARD-000001")
                .cardType(PredefinedLevel5BusinessSync.VIRTUAL_QR)
                .status(PredefinedLevel5BusinessSync.INACTIVE)
                .sourceVersion(1L)
                .syncedAt(LocalDateTime.now().minusDays(1))
                .build();
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(existing));
        when(cardRepository.save(existing)).thenReturn(existing);

        var result = service.processSync(Level5BusinessSyncMessage.builder()
                .syncType(PredefinedLevel5BusinessSync.CARD_STATUS_CHANGED)
                .items(List.of(Level5BusinessSyncItemMessage.builder()
                        .externalId("CARD-000001")
                        .sourceVersion(2L)
                        .card(cardPayload())
                        .build()))
                .build());

        assertEquals(1, result.getUpdatedCount());
        assertEquals(PredefinedLevel5BusinessSync.ACTIVE, existing.getStatus());
        assertEquals(2L, existing.getSourceVersion());
    }

    @Test
    void processSync_SameVersion_IgnoresItem() {
        Card existing = Card.builder()
                .id("CARD-000001")
                .cardType(PredefinedLevel5BusinessSync.VIRTUAL_QR)
                .status(PredefinedLevel5BusinessSync.ACTIVE)
                .sourceVersion(2L)
                .syncedAt(LocalDateTime.now())
                .build();
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(existing));

        var result = service.processSync(Level5BusinessSyncMessage.builder()
                .syncType(PredefinedLevel5BusinessSync.CARD_UPSERT)
                .items(List.of(Level5BusinessSyncItemMessage.builder()
                        .externalId("CARD-000001")
                        .sourceVersion(2L)
                        .card(cardPayload())
                        .build()))
                .build());

        assertEquals(1, result.getIgnoredCount());
        assertEquals(PredefinedLevel5BusinessSync.IGNORED_SAME_VERSION, result.getItems().get(0).getResult());
        verify(cardRepository, never()).save(any());
    }

    @Test
    void processSync_StaleVersion_IgnoresItem() {
        Card existing = Card.builder()
                .id("CARD-000001")
                .cardType(PredefinedLevel5BusinessSync.VIRTUAL_QR)
                .status(PredefinedLevel5BusinessSync.ACTIVE)
                .sourceVersion(3L)
                .syncedAt(LocalDateTime.now())
                .build();
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(existing));

        var result = service.processSync(Level5BusinessSyncMessage.builder()
                .syncType(PredefinedLevel5BusinessSync.CARD_UPSERT)
                .items(List.of(Level5BusinessSyncItemMessage.builder()
                        .externalId("CARD-000001")
                        .sourceVersion(2L)
                        .card(cardPayload())
                        .build()))
                .build());

        assertEquals(1, result.getIgnoredCount());
        assertEquals(PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION, result.getItems().get(0).getResult());
        assertEquals(3L, result.getItems().get(0).getCurrentVersion());
    }

    @Test
    void processSync_TicketReferencesMissingCard_RejectsItem() {
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.empty());

        var result = service.processSync(Level5BusinessSyncMessage.builder()
                .syncType(PredefinedLevel5BusinessSync.TICKET_UPSERT)
                .items(List.of(Level5BusinessSyncItemMessage.builder()
                        .externalId("TICKET-000001")
                        .sourceVersion(1L)
                        .ticket(ticketPayload())
                        .build()))
                .build());

        assertEquals(1, result.getRejectedCount());
        assertEquals("CARD_NOT_SYNCED", result.getItems().get(0).getErrorCode());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void processSync_NewEntitlement_CreatesEntitlement() {
        Card card = Card.builder().id("CARD-000001").sourceVersion(1L).build();
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(card));
        when(entitlementRepository.findById("ENT-000001")).thenReturn(Optional.empty());
        when(entitlementRepository.save(any(Entitlement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.processSync(Level5BusinessSyncMessage.builder()
                .syncType(PredefinedLevel5BusinessSync.ENTITLEMENT_UPSERT)
                .items(List.of(Level5BusinessSyncItemMessage.builder()
                        .externalId("ENT-000001")
                        .sourceVersion(1L)
                        .entitlement(entitlementPayload())
                        .build()))
                .build());

        assertEquals(1, result.getCreatedCount());
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getItems().get(0).getResult());
        verify(entitlementRepository).save(any(Entitlement.class));
    }

    private Level5CardPayload cardPayload() {
        return Level5CardPayload.builder()
                .cardId("CARD-000001")
                .cardType(PredefinedLevel5BusinessSync.VIRTUAL_QR)
                .status(PredefinedLevel5BusinessSync.ACTIVE)
                .build();
    }

    private Level5TicketPayload ticketPayload() {
        return Level5TicketPayload.builder()
                .ticketId("TICKET-000001")
                .cardId("CARD-000001")
                .ticketType(PredefinedLevel5BusinessSync.METRO_SINGLE_RIDE)
                .routeScopeType(PredefinedLevel5BusinessSync.SINGLE_ROUTE)
                .operatorRef("METRO-HCMC")
                .routeRef("METRO-001")
                .transportType(PredefinedTransportType.METRO)
                .usageStatus(PredefinedLevel5BusinessSync.UNUSED)
                .validFrom(LocalDateTime.now().minusMinutes(5))
                .validTo(LocalDateTime.now().plusDays(1))
                .build();
    }

    private Level5EntitlementPayload entitlementPayload() {
        return Level5EntitlementPayload.builder()
                .entitlementId("ENT-000001")
                .cardId("CARD-000001")
                .fareProductCode(PredefinedLevel5BusinessSync.MONTHLY_PASS)
                .passPeriod(PredefinedLevel5BusinessSync.MONTH)
                .passScope(PredefinedLevel5BusinessSync.SINGLE_ROUTE)
                .operatorRef("METRO-HCMC")
                .routeRef("METRO-001")
                .transportType(PredefinedTransportType.METRO)
                .status(PredefinedLevel5BusinessSync.ACTIVE)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusMonths(1))
                .build();
    }
}
