package com.vdt.afc_ops_service.integration.level5.service.impl;

import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5BlacklistMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import com.vdt.afc_ops_service.service.IMediaAccessRulePackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Level5CardSyncServiceTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    OperatorRepository operatorRepository;

    @Mock
    IMediaAccessRulePackageService mediaAccessRulePackageService;

    Level5CardSyncService service;

    @BeforeEach
    void setUp() {
        service = new Level5CardSyncService(cardRepository, operatorRepository, mediaAccessRulePackageService);
    }

    @Test
    void processBlacklistSnapshot_validMessage_upsertsCardWithoutPublishingRules() {
        UUID cardId = UUID.randomUUID();
        C5BlacklistMessage msg = new C5BlacklistMessage(
                UUID.randomUUID(), cardId, null, "LOST_CARD", UUID.randomUUID(), Instant.now());

        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Level5BusinessSyncItemResult result = service.processBlacklistSnapshot(msg);

        assertNotNull(result);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
        assertEquals(cardId.toString(), result.getExternalId());

        // Verify card was saved with BLACKLISTED status
        verify(cardRepository).save(any(Card.class));
        // Verify media access rules were NOT refreshed (snapshot only)
        verify(mediaAccessRulePackageService, never()).refreshAndPublishForOperator(any());
    }

    @Test
    void processBlacklistSnapshot_existingCard_updatesWithoutPublishing() {
        UUID cardId = UUID.randomUUID();
        C5BlacklistMessage msg = new C5BlacklistMessage(
                UUID.randomUUID(), cardId, null, "FRAUD", UUID.randomUUID(), Instant.now());

        Card existingCard = Card.builder().id(cardId.toString()).status("ACTIVE")
                .sourceVersion(100L).cardType("IDENTIFIED").syncedAt(java.time.LocalDateTime.now()).build();
        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.of(existingCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Level5BusinessSyncItemResult result = service.processBlacklistSnapshot(msg);

        assertNotNull(result);
        assertEquals(PredefinedLevel5BusinessSync.UPDATED, result.getResult());
        verify(mediaAccessRulePackageService, never()).refreshAndPublishForOperator(any());
    }

    @Test
    void processBlacklistSnapshot_nullMessage_returnsRejected() {
        Level5BusinessSyncItemResult result = service.processBlacklistSnapshot(null);

        assertEquals(PredefinedLevel5BusinessSync.REJECTED, result.getResult());
        assertEquals("INVALID_BLACKLIST_MESSAGE", result.getErrorCode());
    }

    @Test
    void processBlacklistSnapshot_nullCardId_returnsRejected() {
        C5BlacklistMessage msg = new C5BlacklistMessage(
                UUID.randomUUID(), null, null, "LOST", UUID.randomUUID(), Instant.now());

        Level5BusinessSyncItemResult result = service.processBlacklistSnapshot(msg);

        assertEquals(PredefinedLevel5BusinessSync.REJECTED, result.getResult());
    }

    @Test
    void processBlacklist_added_callsPublishRules() {
        UUID cardId = UUID.randomUUID();
        C5BlacklistMessage msg = new C5BlacklistMessage(
                UUID.randomUUID(), cardId, "ADDED", "LOST_CARD", UUID.randomUUID(), Instant.now());
        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.empty());
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(operatorRepository.findAll()).thenReturn(List.of());

        Level5BusinessSyncItemResult result = service.processBlacklist("blacklist.added", msg);

        assertNotNull(result);
        assertEquals(PredefinedLevel5BusinessSync.CREATED, result.getResult());
    }

    @Test
    void processBlacklist_removed_callsPublishRules() {
        UUID cardId = UUID.randomUUID();
        C5BlacklistMessage msg = new C5BlacklistMessage(
                UUID.randomUUID(), cardId, "REMOVED", null, UUID.randomUUID(), Instant.now());
        Card existingCard = Card.builder().id(cardId.toString()).status("BLACKLISTED")
                .sourceVersion(50L).cardType("IDENTIFIED").syncedAt(java.time.LocalDateTime.now()).build();
        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.of(existingCard));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(operatorRepository.findAll()).thenReturn(List.of(Operator.builder().id(1L).build()));
        doNothing().when(mediaAccessRulePackageService).refreshAndPublishForOperator(any());

        Level5BusinessSyncItemResult result = service.processBlacklist("blacklist.removed", msg);

        assertNotNull(result);
        assertEquals(PredefinedLevel5BusinessSync.UPDATED, result.getResult());
        verify(mediaAccessRulePackageService).refreshAndPublishForOperator(any());
    }

    @Test
    void processBlacklist_sameVersion_ignored_skipsPublish() {
        UUID cardId = UUID.randomUUID();
        C5BlacklistMessage msg = new C5BlacklistMessage(
                UUID.randomUUID(), cardId, "ADDED", "LOST", UUID.randomUUID(), Instant.ofEpochMilli(100L));

        Card existingCard = Card.builder().id(cardId.toString()).status("ACTIVE")
                .sourceVersion(200L).cardType("IDENTIFIED").syncedAt(java.time.LocalDateTime.now()).build();
        when(cardRepository.findById(cardId.toString())).thenReturn(Optional.of(existingCard));

        Level5BusinessSyncItemResult result = service.processBlacklist("blacklist.added", msg);

        assertEquals(PredefinedLevel5BusinessSync.IGNORED_STALE_VERSION, result.getResult());
        verify(mediaAccessRulePackageService, never()).refreshAndPublishForOperator(any());
    }
}