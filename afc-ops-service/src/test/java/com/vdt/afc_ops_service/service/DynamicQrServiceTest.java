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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamicQrServiceTest {

    @Mock
    TicketRepository ticketRepository;

    @Mock
    DynamicQrSessionStore dynamicQrSessionStore;

    DynamicQrService service;

    @BeforeEach
    void setUp() {
        service = new DynamicQrService(ticketRepository,
                dynamicQrSessionStore, new DynamicQrMapper());
        ReflectionTestUtils.setField(service, "ttlSeconds", 30);
        ReflectionTestUtils.setField(service, "qrHmacSecret", "test-secret");
    }

    @Test
    void generate_ActiveTicket_ReturnsSessionPayloadAndCachesQr() {
        Ticket ticket = activeTicket("TICKET-000001", null);
        ticket.setType("SINGLE_TRIP"); // match service check
        when(ticketRepository.findById("TICKET-000001")).thenReturn(Optional.of(ticket));
        when(dynamicQrSessionStore.buildHmacSignedPayload(anyString(), anyLong(), anyString()))
                .thenAnswer(invocation -> "AFCQR:v1:" + invocation.getArgument(0) + ":exp=123:hmac=abc");

        var response = service.generate(GenerateDynamicQrRequest.builder().ticketId("TICKET-000001").build());

        assertEquals(30, response.getRefreshAfterSeconds());
        assertNotNull(response.getQrId());
        ArgumentCaptor<DynamicQrSession> sessionCaptor = ArgumentCaptor.forClass(DynamicQrSession.class);
        verify(dynamicQrSessionStore).create(eq(response.getQrId()), sessionCaptor.capture(), eq(30L));
        assertEquals(null, sessionCaptor.getValue().cardId());
        assertEquals("TICKET-000001", sessionCaptor.getValue().ticketId());
        assertEquals(null, sessionCaptor.getValue().entitlementId());
        assertEquals(false, sessionCaptor.getValue().used());
    }

    @Test
    void generate_ActiveTicketWithCard_ValidatesCardAndReturnsSession() {
        Card card = activeCard("CARD-000001");
        Ticket ticket = activeTicket("TICKET-000001", "CARD-000001");
        ticket.setCard(card);
        when(ticketRepository.findById("TICKET-000001")).thenReturn(Optional.of(ticket));
        when(dynamicQrSessionStore.buildHmacSignedPayload(anyString(), anyLong(), anyString()))
                .thenReturn("AFCQR:v1:TICKET-000001:exp=123:hmac=abc");

        var response = service.generate(GenerateDynamicQrRequest.builder().ticketId("TICKET-000001").build());

        ArgumentCaptor<DynamicQrSession> sessionCaptor = ArgumentCaptor.forClass(DynamicQrSession.class);
        verify(dynamicQrSessionStore).create(eq(response.getQrId()), sessionCaptor.capture(), eq(30L));
        assertEquals("CARD-000001", sessionCaptor.getValue().cardId());
    }

    @Test
    void generate_TicketNotFound_ThrowsTicketNotFound() {
        when(ticketRepository.findById("TICKET-000001")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().ticketId(" TICKET-000001 ").build()));

        assertEquals(ErrorCode.TICKET_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void generate_TicketExpired_ThrowsTicketExpired() {
        Ticket ticket = activeTicket("TICKET-000001", null);
        ticket.setUsageStatus(PredefinedLevel5BusinessSync.EXPIRED);
        when(ticketRepository.findById("TICKET-000001")).thenReturn(Optional.of(ticket));

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().ticketId("TICKET-000001").build()));

        assertEquals(ErrorCode.TICKET_EXPIRED, exception.getErrorCode());
    }

    @Test
    void generate_TicketAlreadyUsed_ThrowsTicketAlreadyUsed() {
        Ticket ticket = activeTicket("TICKET-000001", null);
        ticket.setUsageStatus(PredefinedLevel5BusinessSync.USED);
        when(ticketRepository.findById("TICKET-000001")).thenReturn(Optional.of(ticket));

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().ticketId("TICKET-000001").build()));

        assertEquals(ErrorCode.TICKET_ALREADY_USED, exception.getErrorCode());
    }

    @Test
    void generate_TicketWithCancelledCard_ThrowsCardCancelled() {
        Card card = activeCard("CARD-000001");
        card.setStatus(PredefinedLevel5BusinessSync.CANCELLED);
        Ticket ticket = activeTicket("TICKET-000001", "CARD-000001");
        ticket.setCard(card);
        when(ticketRepository.findById("TICKET-000001")).thenReturn(Optional.of(ticket));

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().ticketId("TICKET-000001").build()));

        assertEquals(ErrorCode.CARD_CANCELLED, exception.getErrorCode());
    }

    @Test
    void generate_TicketWithBlacklistedCard_ThrowsMediaBlacklisted() {
        Card card = activeCard("CARD-000001");
        card.setStatus(PredefinedLevel5BusinessSync.BLACKLISTED);
        Ticket ticket = activeTicket("TICKET-000001", "CARD-000001");
        ticket.setCard(card);
        when(ticketRepository.findById("TICKET-000001")).thenReturn(Optional.of(ticket));

        AppException exception = assertThrows(AppException.class, () -> service.generate(
                GenerateDynamicQrRequest.builder().ticketId("TICKET-000001").build()));

        assertEquals(ErrorCode.MEDIA_BLACKLISTED, exception.getErrorCode());
    }

    @Test
    void generate_MonthlyPassTicket_ReturnsSessionWithEntitlementId() {
        Ticket ticket = monthlyPassTicket("ENT-000001", null);
        when(ticketRepository.findById("ENT-000001")).thenReturn(Optional.of(ticket));
        when(dynamicQrSessionStore.buildHmacSignedPayload(anyString(), anyLong(), anyString()))
                .thenReturn("AFCQR:v1:ENT-000001:exp=123:hmac=abc");

        var response = service.generate(GenerateDynamicQrRequest.builder().ticketId("ENT-000001").build());

        ArgumentCaptor<DynamicQrSession> sessionCaptor = ArgumentCaptor.forClass(DynamicQrSession.class);
        verify(dynamicQrSessionStore).create(eq(response.getQrId()), sessionCaptor.capture(), eq(30L));
        assertEquals(null, sessionCaptor.getValue().ticketId());
        assertEquals("ENT-000001", sessionCaptor.getValue().entitlementId());
    }

    private Card activeCard(String cardId) {
        return Card.builder().id(cardId).cardType(PredefinedLevel5BusinessSync.IDENTIFIED)
                .status(PredefinedLevel5BusinessSync.ACTIVE).sourceVersion(1L).syncedAt(LocalDateTime.now()).build();
    }

    private Ticket activeTicket(String ticketId, String cardId) {
        return Ticket.builder().id(ticketId)
                .card(cardId != null ? Card.builder().id(cardId).build() : null)
                .type("METRO_SINGLE_RIDE")
                .usageStatus(PredefinedLevel5BusinessSync.UNUSED)
                .validFrom(LocalDateTime.now().minusMinutes(5)).validTo(LocalDateTime.now().plusDays(1))
                .sourceVersion(1L).syncedAt(LocalDateTime.now()).build();
    }

    private Ticket monthlyPassTicket(String ticketId, String cardId) {
        return Ticket.builder().id(ticketId)
                .card(cardId != null ? Card.builder().id(cardId).build() : null)
                .type(PredefinedLevel5BusinessSync.MONTHLY_PASS)
                .usageStatus(PredefinedLevel5BusinessSync.ACTIVE)
                .validFrom(LocalDateTime.now().minusDays(1)).validTo(LocalDateTime.now().plusMonths(1))
                .sourceVersion(1L).syncedAt(LocalDateTime.now()).build();
    }
}