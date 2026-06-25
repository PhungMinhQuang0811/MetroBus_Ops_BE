package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.dto.request.qr.GenerateDynamicQrRequest;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.mapper.DynamicQrMapper;
import com.vdt.afc_ops_service.qr.DynamicQrSession;
import com.vdt.afc_ops_service.qr.DynamicQrSessionStore;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.TicketRepository;
import com.vdt.afc_ops_service.service.Impl.DynamicQrService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicQrServiceTest {

    @Mock
    CardRepository cardRepository;

    @Mock
    TicketRepository ticketRepository;

    @Mock
    DynamicQrSessionStore dynamicQrSessionStore;

    DynamicQrService service;

    @BeforeEach
    void setUp() {
        service = new DynamicQrService(cardRepository, ticketRepository,
                dynamicQrSessionStore, new DynamicQrMapper());
        ReflectionTestUtils.setField(service, "ttlSeconds", 30);
    }

    @Test
    void generate_ActiveCardWithTicket_ReturnsSessionPayloadAndCachesQr() {
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(ticketRepository.findAllByCardIdAndUsageStatusInAndValidToAfter(
                eq("CARD-000001"), any(), any(LocalDateTime.class)
        )).thenReturn(List.of(activeTicket("TICKET-000001", "CARD-000001")));
        when(ticketRepository.findAllByCardIdAndTypeAndUsageStatusAndValidToAfter(
                eq("CARD-000001"), eq("MONTHLY_PASS"), eq(PredefinedLevel5BusinessSync.ACTIVE), any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(dynamicQrSessionStore.buildPayload(any())).thenAnswer(invocation -> "AFCQR:v1:" + invocation.getArgument(0));

        var response = service.generate(GenerateDynamicQrRequest.builder().cardId("CARD-000001").build());

        assertEquals(30, response.getRefreshAfterSeconds());
        assertNotNull(response.getQrId());
        assertEquals("AFCQR:v1:" + response.getQrId(), response.getQrPayload());
        ArgumentCaptor<DynamicQrSession> sessionCaptor = ArgumentCaptor.forClass(DynamicQrSession.class);
        verify(dynamicQrSessionStore).create(eq(response.getQrId()), sessionCaptor.capture(), eq(30L));
        assertEquals("CARD-000001", sessionCaptor.getValue().cardId());
        assertEquals("TICKET-000001", sessionCaptor.getValue().ticketId());
        assertEquals(null, sessionCaptor.getValue().entitlementId());
        assertEquals(false, sessionCaptor.getValue().used());
    }

    @Test
    void generate_ActiveCardWithMonthlyPass_ReturnsSessionPayloadAndCachesQr() {
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(ticketRepository.findAllByCardIdAndUsageStatusInAndValidToAfter(
                eq("CARD-000001"), any(), any(LocalDateTime.class)
        )).thenReturn(List.of());
        when(ticketRepository.findAllByCardIdAndTypeAndUsageStatusAndValidToAfter(
                eq("CARD-000001"), eq("MONTHLY_PASS"), eq(PredefinedLevel5BusinessSync.ACTIVE), any(LocalDateTime.class)
        )).thenReturn(List.of(monthlyPassTicket("ENT-000001", "CARD-000001")));
        when(dynamicQrSessionStore.buildPayload(any())).thenAnswer(invocation -> "AFCQR:v1:" + invocation.getArgument(0));

        var response = service.generate(GenerateDynamicQrRequest.builder().cardId("CARD-000001").build());

        ArgumentCaptor<DynamicQrSession> sessionCaptor = ArgumentCaptor.forClass(DynamicQrSession.class);
        verify(dynamicQrSessionStore).create(eq(response.getQrId()), sessionCaptor.capture(), eq(30L));
        assertEquals("AFCQR:v1:" + response.getQrId(), response.getQrPayload());
        assertEquals(null, sessionCaptor.getValue().ticketId());
        assertEquals("ENT-000001", sessionCaptor.getValue().entitlementId());
    }

    @Test
    void generate_CardNotFound_ThrowsCardNotFound() {
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().cardId(" CARD-000001 ").build()));

        assertEquals(ErrorCode.CARD_NOT_FOUND, exception.getErrorCode());
        verify(ticketRepository, never()).findAllByCardIdAndUsageStatusInAndValidToAfter(any(), any(), any());
    }

    @Test
    void generate_CardInactive_ThrowsCardInactive() {
        Card card = activeCard("CARD-000001");
        card.setStatus(PredefinedLevel5BusinessSync.INACTIVE);
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(card));

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().cardId("CARD-000001").build()));

        assertEquals(ErrorCode.CARD_INACTIVE, exception.getErrorCode());
    }

    @Test
    void generate_CardBlacklisted_ThrowsMediaBlacklisted() {
        Card card = activeCard("CARD-000001");
        card.setStatus(PredefinedLevel5BusinessSync.BLACKLISTED);
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(card));

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().cardId("CARD-000001").build()));

        assertEquals(ErrorCode.MEDIA_BLACKLISTED, exception.getErrorCode());
    }

    @Test
    void generate_CardCancelled_ThrowsCardCancelled() {
        Card card = activeCard("CARD-000001");
        card.setStatus(PredefinedLevel5BusinessSync.CANCELLED);
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(card));

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().cardId("CARD-000001").build()));

        assertEquals(ErrorCode.CARD_CANCELLED, exception.getErrorCode());
    }

    @Test
    void generate_NoActiveProduct_ThrowsActiveProductNotFound() {
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(ticketRepository.findAllByCardIdAndUsageStatusInAndValidToAfter(
                eq("CARD-000001"), any(), any(LocalDateTime.class))).thenReturn(List.of());
        when(ticketRepository.findAllByCardIdAndTypeAndUsageStatusAndValidToAfter(
                eq("CARD-000001"), eq("MONTHLY_PASS"), eq(PredefinedLevel5BusinessSync.ACTIVE), any(LocalDateTime.class)
        )).thenReturn(List.of());

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().cardId("CARD-000001").build()));

        assertEquals(ErrorCode.ACTIVE_PRODUCT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void generate_TicketAndMonthlyPassActive_ThrowsConflict() {
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(ticketRepository.findAllByCardIdAndUsageStatusInAndValidToAfter(
                eq("CARD-000001"), any(), any(LocalDateTime.class)
        )).thenReturn(List.of(activeTicket("TICKET-000001", "CARD-000001")));
        when(ticketRepository.findAllByCardIdAndTypeAndUsageStatusAndValidToAfter(
                eq("CARD-000001"), eq("MONTHLY_PASS"), eq(PredefinedLevel5BusinessSync.ACTIVE), any(LocalDateTime.class)
        )).thenReturn(List.of(monthlyPassTicket("ENT-000001", "CARD-000001")));

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().cardId("CARD-000001").build()));

        assertEquals(ErrorCode.ACTIVE_PRODUCT_CONFLICT, exception.getErrorCode());
    }

    @Test
    void generate_MultipleTicketsActive_ThrowsConflict() {
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(ticketRepository.findAllByCardIdAndUsageStatusInAndValidToAfter(
                eq("CARD-000001"), any(), any(LocalDateTime.class)
        )).thenReturn(List.of(activeTicket("TICKET-000001", "CARD-000001"), activeTicket("TICKET-000002", "CARD-000001")));
        when(ticketRepository.findAllByCardIdAndTypeAndUsageStatusAndValidToAfter(
                eq("CARD-000001"), eq("MONTHLY_PASS"), eq(PredefinedLevel5BusinessSync.ACTIVE), any(LocalDateTime.class)
        )).thenReturn(List.of());

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().cardId("CARD-000001").build()));

        assertEquals(ErrorCode.ACTIVE_PRODUCT_CONFLICT, exception.getErrorCode());
    }

    @Test
    void generate_MultipleMonthlyPassesActive_ThrowsConflict() {
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(ticketRepository.findAllByCardIdAndUsageStatusInAndValidToAfter(
                eq("CARD-000001"), any(), any(LocalDateTime.class))).thenReturn(List.of());
        when(ticketRepository.findAllByCardIdAndTypeAndUsageStatusAndValidToAfter(
                eq("CARD-000001"), eq("MONTHLY_PASS"), eq(PredefinedLevel5BusinessSync.ACTIVE), any(LocalDateTime.class)
        )).thenReturn(List.of(monthlyPassTicket("ENT-000001", "CARD-000001"), monthlyPassTicket("ENT-000002", "CARD-000001")));

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().cardId("CARD-000001").build()));

        assertEquals(ErrorCode.ACTIVE_PRODUCT_CONFLICT, exception.getErrorCode());
    }

    private Card activeCard(String cardId) {
        return Card.builder().id(cardId).cardType(PredefinedLevel5BusinessSync.IDENTIFIED)
                .status(PredefinedLevel5BusinessSync.ACTIVE).sourceVersion(1L).syncedAt(LocalDateTime.now()).build();
    }

    private Ticket activeTicket(String ticketId, String cardId) {
        return Ticket.builder().id(ticketId).card(Card.builder().id(cardId).build())
                .type(PredefinedLevel5BusinessSync.METRO_SINGLE_RIDE)
                .usageStatus(PredefinedLevel5BusinessSync.UNUSED)
                .validFrom(LocalDateTime.now().minusMinutes(5)).validTo(LocalDateTime.now().plusDays(1))
                .sourceVersion(1L).syncedAt(LocalDateTime.now()).build();
    }

    private Ticket monthlyPassTicket(String ticketId, String cardId) {
        return Ticket.builder().id(ticketId).card(Card.builder().id(cardId).build())
                .type(PredefinedLevel5BusinessSync.MONTHLY_PASS)
                .usageStatus(PredefinedLevel5BusinessSync.ACTIVE)
                .validFrom(LocalDateTime.now().minusDays(1)).validTo(LocalDateTime.now().plusMonths(1))
                .sourceVersion(1L).syncedAt(LocalDateTime.now()).build();
    }
}