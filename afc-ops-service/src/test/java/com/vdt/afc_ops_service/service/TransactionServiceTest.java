package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedDeviceDirection;
import com.vdt.afc_ops_service.constant.PredefinedDeviceStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransactionDecision;
import com.vdt.afc_ops_service.constant.PredefinedTransactionReason;
import com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest;
import com.vdt.afc_ops_service.entity.Transaction;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Entitlement;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.mapper.TransactionMapper;
import com.vdt.afc_ops_service.qr.DynamicQrSession;
import com.vdt.afc_ops_service.qr.DynamicQrSessionStore;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.EntitlementRepository;
import com.vdt.afc_ops_service.repository.TicketRepository;
import com.vdt.afc_ops_service.service.Impl.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    DeviceRepository deviceRepository;

    @Mock
    CardRepository cardRepository;

    @Mock
    TicketRepository ticketRepository;

    @Mock
    EntitlementRepository entitlementRepository;

    @Mock
    TransactionRepository transactionRepository;

    @Mock
    DynamicQrSessionStore dynamicQrSessionStore;

    TransactionService service;

    @BeforeEach
    void setUp() {
        service = new TransactionService(deviceRepository, cardRepository, ticketRepository,
                entitlementRepository, transactionRepository, dynamicQrSessionStore, new TransactionMapper());
        lenient().when(transactionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void submit_ValidEntryTicket_OpensGateMarksTicketInUseAndQrUsed() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        Ticket ticket = ticket("TICKET-000001", "CARD-000001", PredefinedLevel5BusinessSync.UNUSED);
        when(ticketRepository.findByIdAndCardId("TICKET-000001", "CARD-000001")).thenReturn(Optional.of(ticket));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.OPEN_GATE, response.getDecision());
        assertEquals(PredefinedTransactionReason.VALID, response.getReason());
        assertEquals(PredefinedLevel5BusinessSync.IN_USE, ticket.getUsageStatus());
        assertNotNull(ticket.getFirstTapAt());
        verify(ticketRepository).save(ticket);
        verify(dynamicQrSessionStore).markUsed(eq("QR-000001"), any(DynamicQrSession.class));
        Transaction transaction = savedTransaction();
        assertEquals(PredefinedTransactionDecision.OPEN_GATE, transaction.getDecision());
        assertEquals("TAP_IN", transaction.getTapType());
        assertEquals("CARD-000001", transaction.getCard().getId());
        assertEquals("TICKET-000001", transaction.getTicket().getId());
        assertNotNull(transaction.getQrPayloadHash());
    }

    @Test
    void submit_ValidExitTicket_OpensGateMarksTicketUsed() {
        mockDevice(activeDevice(PredefinedDeviceDirection.EXIT));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        Ticket ticket = ticket("TICKET-000001", "CARD-000001", PredefinedLevel5BusinessSync.IN_USE);
        when(ticketRepository.findByIdAndCardId("TICKET-000001", "CARD-000001")).thenReturn(Optional.of(ticket));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.OPEN_GATE, response.getDecision());
        assertEquals(PredefinedLevel5BusinessSync.USED, ticket.getUsageStatus());
        assertNotNull(ticket.getUsedAt());
        assertEquals("TAP_OUT", savedTransaction().getTapType());
    }

    @Test
    void submit_ValidEntitlement_OpensGateWithoutChangingEntitlement() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", null, "ENT-000001", false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(entitlementRepository.findByIdAndCardId("ENT-000001", "CARD-000001"))
                .thenReturn(Optional.of(entitlement("ENT-000001", "CARD-000001", PredefinedLevel5BusinessSync.ACTIVE)));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.OPEN_GATE, response.getDecision());
        assertEquals("ENT-000001", savedTransaction().getEntitlement().getId());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void submit_ValidTicketWithNoQrExpiry_OpensGate() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(new DynamicQrSession("CARD-000001", "TICKET-000001", null, null, false));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        Ticket ticket = ticket("TICKET-000001", "CARD-000001", PredefinedLevel5BusinessSync.UNUSED);
        when(ticketRepository.findByIdAndCardId("TICKET-000001", "CARD-000001")).thenReturn(Optional.of(ticket));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.OPEN_GATE, response.getDecision());
        verify(dynamicQrSessionStore).markUsed(eq("QR-000001"), any(DynamicQrSession.class));
    }

    @Test
    void submit_DeviceNotFound_ThrowsDeviceNotFound() {
        when(deviceRepository.findByDeviceCode("GATE-001")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> service.submit(request()));

        assertEquals(ErrorCode.DEVICE_NOT_FOUND, exception.getErrorCode());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void submit_DeviceDisabled_DeniesAndStoresTransaction() {
        Device device = activeDevice(PredefinedDeviceDirection.BOTH);
        device.setStatus(PredefinedDeviceStatus.DISABLED);
        mockDevice(device);

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.DENY, response.getDecision());
        assertEquals(PredefinedTransactionReason.DEVICE_DISABLED, response.getReason());
        verify(dynamicQrSessionStore, never()).parseQrId(any());
        assertEquals(PredefinedTransactionReason.DEVICE_DISABLED, savedTransaction().getReason());
    }

    @Test
    void submit_InvalidDirection_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.BOTH));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.DENY, response.getDecision());
        assertEquals(PredefinedTransactionReason.INVALID_DIRECTION, response.getReason());
    }

    @Test
    void submit_InvalidQrPayload_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));

        var response = service.submit(requestWithQr("not-a-qr"));

        assertEquals(PredefinedTransactionDecision.DENY, response.getDecision());
        assertEquals(PredefinedTransactionReason.QR_INVALID, response.getReason());
    }

    @Test
    void submit_ExpiredMissingQrSession_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        when(dynamicQrSessionStore.parseQrId("AFCQR:v1:QR-000001")).thenReturn("QR-000001");
        when(dynamicQrSessionStore.find("QR-000001")).thenReturn(null);

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.DENY, response.getDecision());
        assertEquals(PredefinedTransactionReason.QR_EXPIRED, response.getReason());
    }

    @Test
    void submit_ExpiredQrTimestamp_DeniesBeforeCardLookup() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, -1));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.DENY, response.getDecision());
        assertEquals(PredefinedTransactionReason.QR_EXPIRED, response.getReason());
        verify(cardRepository, never()).findById(any());
    }

    @Test
    void submit_ReplayedQr_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, true, 60));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.DENY, response.getDecision());
        assertEquals(PredefinedTransactionReason.QR_REPLAYED, response.getReason());
    }

    @Test
    void submit_QrSessionWithoutCard_DeniesUnknownMedia() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session(null, "TICKET-000001", null, false, 60));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.UNKNOWN_MEDIA, response.getReason());
    }

    @Test
    void submit_CardNotFound_DeniesUnknownMedia() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.empty());

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.UNKNOWN_MEDIA, response.getReason());
    }

    @Test
    void submit_BlacklistedCard_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        Card card = activeCard("CARD-000001");
        card.setStatus(PredefinedLevel5BusinessSync.BLACKLISTED);
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(card));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionDecision.DENY, response.getDecision());
        assertEquals(PredefinedTransactionReason.MEDIA_BLACKLISTED, response.getReason());
    }

    @Test
    void submit_CancelledCard_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        Card card = activeCard("CARD-000001");
        card.setStatus(PredefinedLevel5BusinessSync.CANCELLED);
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(card));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.CARD_CANCELLED, response.getReason());
    }

    @Test
    void submit_InactiveCard_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        Card card = activeCard("CARD-000001");
        card.setStatus(PredefinedLevel5BusinessSync.INACTIVE);
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(card));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.CARD_INACTIVE, response.getReason());
    }

    @Test
    void submit_TicketExpired_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        Ticket ticket = ticket("TICKET-000001", "CARD-000001", PredefinedLevel5BusinessSync.UNUSED);
        ticket.setValidTo(LocalDateTime.now().minusMinutes(1));
        when(ticketRepository.findByIdAndCardId("TICKET-000001", "CARD-000001")).thenReturn(Optional.of(ticket));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.TICKET_EXPIRED, response.getReason());
    }

    @Test
    void submit_TicketAlreadyUsed_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(ticketRepository.findByIdAndCardId("TICKET-000001", "CARD-000001"))
                .thenReturn(Optional.of(ticket("TICKET-000001", "CARD-000001", PredefinedLevel5BusinessSync.USED)));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.TICKET_ALREADY_USED, response.getReason());
    }

    @Test
    void submit_EntitlementExpired_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", null, "ENT-000001", false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        Entitlement entitlement = entitlement("ENT-000001", "CARD-000001", PredefinedLevel5BusinessSync.ACTIVE);
        entitlement.setValidTo(LocalDateTime.now().minusMinutes(1));
        when(entitlementRepository.findByIdAndCardId("ENT-000001", "CARD-000001")).thenReturn(Optional.of(entitlement));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.ENTITLEMENT_EXPIRED, response.getReason());
    }

    @Test
    void submit_EntitlementInactive_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", null, "ENT-000001", false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(entitlementRepository.findByIdAndCardId("ENT-000001", "CARD-000001"))
                .thenReturn(Optional.of(entitlement("ENT-000001", "CARD-000001", PredefinedLevel5BusinessSync.INACTIVE)));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.ENTITLEMENT_INACTIVE, response.getReason());
    }

    @Test
    void submit_ProductConflictInQrSession_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", "ENT-000001", false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.ACTIVE_PRODUCT_CONFLICT, response.getReason());
    }

    @Test
    void submit_NoActiveProductInQrSession_Denies() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", null, null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.ACTIVE_PRODUCT_NOT_FOUND, response.getReason());
    }

    @Test
    void submit_TicketNotFound_DeniesInvalidTicket() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(ticketRepository.findByIdAndCardId("TICKET-000001", "CARD-000001")).thenReturn(Optional.empty());

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.TICKET_INVALID, response.getReason());
    }

    @Test
    void submit_UnusedTicketAtExit_DeniesInvalidTicket() {
        mockDevice(activeDevice(PredefinedDeviceDirection.EXIT));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        Ticket ticket = ticket("TICKET-000001", "CARD-000001", PredefinedLevel5BusinessSync.UNUSED);
        when(ticketRepository.findByIdAndCardId("TICKET-000001", "CARD-000001")).thenReturn(Optional.of(ticket));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.TICKET_INVALID, response.getReason());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void submit_InUseTicketAtEntry_DeniesInvalidTicket() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        Ticket ticket = ticket("TICKET-000001", "CARD-000001", PredefinedLevel5BusinessSync.IN_USE);
        when(ticketRepository.findByIdAndCardId("TICKET-000001", "CARD-000001")).thenReturn(Optional.of(ticket));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.TICKET_INVALID, response.getReason());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void submit_TicketUnknownStatus_DeniesInvalidTicket() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", "TICKET-000001", null, false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(ticketRepository.findByIdAndCardId("TICKET-000001", "CARD-000001"))
                .thenReturn(Optional.of(ticket("TICKET-000001", "CARD-000001", "SUSPENDED")));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.TICKET_INVALID, response.getReason());
    }

    @Test
    void submit_EntitlementNotFound_DeniesInactiveEntitlement() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", null, "ENT-000001", false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(entitlementRepository.findByIdAndCardId("ENT-000001", "CARD-000001")).thenReturn(Optional.empty());

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.ENTITLEMENT_INACTIVE, response.getReason());
    }

    @Test
    void submit_EntitlementStartsInFuture_DeniesExpiredEntitlement() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", null, "ENT-000001", false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        Entitlement entitlement = entitlement("ENT-000001", "CARD-000001", PredefinedLevel5BusinessSync.ACTIVE);
        entitlement.setValidFrom(LocalDateTime.now().plusMinutes(1));
        when(entitlementRepository.findByIdAndCardId("ENT-000001", "CARD-000001")).thenReturn(Optional.of(entitlement));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.ENTITLEMENT_EXPIRED, response.getReason());
    }

    @Test
    void submit_ExpiredEntitlementStatus_DeniesExpiredEntitlement() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));
        mockQrSession(session("CARD-000001", null, "ENT-000001", false, 60));
        when(cardRepository.findById("CARD-000001")).thenReturn(Optional.of(activeCard("CARD-000001")));
        when(entitlementRepository.findByIdAndCardId("ENT-000001", "CARD-000001"))
                .thenReturn(Optional.of(entitlement("ENT-000001", "CARD-000001", PredefinedLevel5BusinessSync.EXPIRED)));

        var response = service.submit(request());

        assertEquals(PredefinedTransactionReason.ENTITLEMENT_EXPIRED, response.getReason());
    }

    @Test
    void submit_BadDeviceSecret_ThrowsUnauthenticated() {
        mockDevice(activeDevice(PredefinedDeviceDirection.ENTRY));

        AppException exception = assertThrows(AppException.class,
                () -> service.submit(SubmitTransactionRequest.builder()
                        .deviceCode("GATE-001")
                        .deviceSecret("bad-secret")
                        .qrPayload("AFCQR:v1:QR-000001")
                        .build()));

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
        verify(transactionRepository, never()).save(any());
    }

    private Transaction savedTransaction() {
        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        return captor.getValue();
    }

    private void mockDevice(Device device) {
        when(deviceRepository.findByDeviceCode("GATE-001")).thenReturn(Optional.of(device));
    }

    private void mockQrSession(DynamicQrSession session) {
        when(dynamicQrSessionStore.parseQrId("AFCQR:v1:QR-000001")).thenReturn("QR-000001");
        when(dynamicQrSessionStore.find("QR-000001")).thenReturn(session);
    }

    private SubmitTransactionRequest request() {
        return requestWithQr("AFCQR:v1:QR-000001");
    }

    private SubmitTransactionRequest requestWithQr(String qrPayload) {
        return SubmitTransactionRequest.builder()
                .deviceCode(" GATE-001 ")
                .deviceSecret("secret")
                .qrPayload(qrPayload)
                .build();
    }

    private DynamicQrSession session(String cardId, String ticketId, String entitlementId, boolean used, long secondsFromNow) {
        long exp = LocalDateTime.now().plusSeconds(secondsFromNow)
                .atZone(java.time.ZoneId.systemDefault())
                .toEpochSecond();
        return new DynamicQrSession(cardId, ticketId, entitlementId, exp, used);
    }

    private Device activeDevice(String direction) {
        Operator operator = Operator.builder()
                .id(1L)
                .operatorCode("OP-001")
                .operatorName("Operator")
                .status("ACTIVE")
                .build();
        Route route = Route.builder()
                .id(1L)
                .operator(operator)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .transportType("METRO")
                .status("ACTIVE")
                .build();
        Station station = Station.builder()
                .id(1L)
                .route(route)
                .stationCode("ST-001")
                .stationName("Ben Thanh")
                .stationOrder(1)
                .status("ACTIVE")
                .build();
        return Device.builder()
                .id(1L)
                .station(station)
                .deviceCode("GATE-001")
                .deviceSecret("secret")
                .deviceType("QR_SCANNER_SIMULATOR")
                .direction(direction)
                .status(PredefinedDeviceStatus.ACTIVE)
                .build();
    }

    private Card activeCard(String cardId) {
        return Card.builder()
                .id(cardId)
                .cardType(PredefinedLevel5BusinessSync.VIRTUAL_QR)
                .status(PredefinedLevel5BusinessSync.ACTIVE)
                .sourceVersion(1L)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    private Ticket ticket(String ticketId, String cardId, String usageStatus) {
        return Ticket.builder()
                .id(ticketId)
                .card(Card.builder().id(cardId).build())
                .ticketType(PredefinedLevel5BusinessSync.METRO_SINGLE_RIDE)
                .routeScopeType(PredefinedLevel5BusinessSync.NETWORK)
                .transportType("METRO")
                .usageStatus(usageStatus)
                .validFrom(LocalDateTime.now().minusMinutes(5))
                .validTo(LocalDateTime.now().plusDays(1))
                .sourceVersion(1L)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    private Entitlement entitlement(String entitlementId, String cardId, String status) {
        return Entitlement.builder()
                .id(entitlementId)
                .card(Card.builder().id(cardId).build())
                .fareProductCode(PredefinedLevel5BusinessSync.MONTHLY_PASS)
                .passPeriod(PredefinedLevel5BusinessSync.MONTH)
                .passScope(PredefinedLevel5BusinessSync.NETWORK)
                .operatorRef("OP-001")
                .routeRef("METRO-001")
                .transportType("METRO")
                .status(status)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusMonths(1))
                .sourceVersion(1L)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}
