package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.common.util.CryptoHashUtil;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedTransactionDecision;
import com.vdt.afc_ops_service.constant.PredefinedTransactionReason;
import com.vdt.afc_ops_service.constant.PredefinedTransactionStatus;
import com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest;
import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionDetailResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionListItemResponse;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.entity.Transaction;
import com.vdt.afc_ops_service.qr.DynamicQrSession;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TransactionMapper {

    public TransactionEvaluation deny(String reason) {
        return new TransactionEvaluation(PredefinedTransactionDecision.DENY, reason,
                null, null, null, null);
    }

    public TransactionEvaluation denyQr(String qrId, DynamicQrSession session, String reason) {
        return new TransactionEvaluation(PredefinedTransactionDecision.DENY, reason,
                qrId, session, null, null);
    }

    public TransactionEvaluation denyCard(String qrId, DynamicQrSession session, Card card, String reason) {
        return new TransactionEvaluation(PredefinedTransactionDecision.DENY, reason,
                qrId, session, card, null);
    }

    public TransactionEvaluation denyTicket(String qrId, DynamicQrSession session, Card card,
                                            Ticket ticket, String reason) {
        return new TransactionEvaluation(PredefinedTransactionDecision.DENY, reason,
                qrId, session, card, ticket);
    }

    public TransactionEvaluation allowTicket(String qrId, DynamicQrSession session, Card card, Ticket ticket) {
        return new TransactionEvaluation(PredefinedTransactionDecision.OPEN_GATE, PredefinedTransactionReason.VALID,
                qrId, session, card, ticket);
    }

    public Transaction toTransaction(SubmitTransactionRequest request, Device device, String direction,
                                     String tapType, TransactionEvaluation evaluation,
                                     String transactionId, LocalDateTime now) {
        return Transaction.builder()
                .id(transactionId)
                .eventId(transactionId)
                .operator(device.getStation().getRoute().getOperator())
                .route(device.getStation().getRoute())
                .station(device.getStation())
                .device(device)
                .mediaType("VIRTUAL_QR")
                .card(evaluation.card())
                .ticket(evaluation.ticket())
                .qrId(evaluation.qrId())
                .qrPayloadHash(CryptoHashUtil.sha256Base64Url(SearchFilterUtil.normalize(request.getQrPayload())))
                .tapType(tapType)
                .occurredAt(now)
                .receivedAt(now)
                .decision(evaluation.decision())
                .reason(evaluation.reason())
                .syncStatus(PredefinedTransactionStatus.PENDING)
                .build();
    }

    public SubmitTransactionResponse toResponse(Transaction transaction, TransactionEvaluation evaluation,
                                                LocalDateTime serverTime) {
        return SubmitTransactionResponse.builder()
                .transactionId(transaction.getId())
                .decision(evaluation.decision())
                .reason(evaluation.reason())
                .serverTime(serverTime)
                .build();
    }

    public TransactionListItemResponse toListItemResponse(Transaction transaction) {
        Route route = transaction.getRoute();
        Station station = transaction.getStation();
        Device device = transaction.getDevice();
        Ticket ticket = transaction.getTicket();

        return TransactionListItemResponse.builder()
                .id(transaction.getId())
                .eventId(transaction.getEventId())
                .routeId(route != null ? route.getId() : null)
                .routeCode(route != null ? route.getRouteCode() : null)
                .routeName(route != null ? route.getRouteName() : null)
                .stationId(station != null ? station.getId() : null)
                .stationCode(station != null ? station.getStationCode() : null)
                .stationName(station != null ? station.getStationName() : null)
                .deviceId(device != null ? device.getId() : null)
                .deviceCode(device != null ? device.getDeviceCode() : null)
                .mediaType(transaction.getMediaType())
                .cardId(getCardId(transaction.getCard()))
                .ticketId(getTicketId(ticket))
                .entitlementId(null)
                .qrId(transaction.getQrId())
                .tapType(transaction.getTapType())
                .occurredAt(transaction.getOccurredAt())
                .decision(transaction.getDecision())
                .reason(transaction.getReason())
                .syncStatus(transaction.getSyncStatus())
                .ticketProcessingStatus(transaction.getTicketProcessingStatus())
                .batchId(transaction.getBatchId())
                .build();
    }

    public TransactionDetailResponse toDetailResponse(Transaction transaction) {
        Operator operator = transaction.getOperator();
        Route route = transaction.getRoute();
        Station station = transaction.getStation();
        Device device = transaction.getDevice();
        Card card = transaction.getCard();
        Ticket ticket = transaction.getTicket();

        return TransactionDetailResponse.builder()
                .id(transaction.getId())
                .eventId(transaction.getEventId())
                .operatorId(operator != null ? operator.getId() : null)
                .operatorCode(operator != null ? operator.getOperatorCode() : null)
                .operatorName(operator != null ? operator.getOperatorName() : null)
                .routeId(route != null ? route.getId() : null)
                .routeCode(route != null ? route.getRouteCode() : null)
                .routeName(route != null ? route.getRouteName() : null)
                .stationId(station != null ? station.getId() : null)
                .stationCode(station != null ? station.getStationCode() : null)
                .stationName(station != null ? station.getStationName() : null)
                .deviceId(device != null ? device.getId() : null)
                .deviceCode(device != null ? device.getDeviceCode() : null)
                .deviceType(device != null ? device.getDeviceType() : null)
                .deviceDirection(device != null ? device.getDirection() : null)
                .mediaType(transaction.getMediaType())
                .cardId(getCardId(card))
                .cardUid(card != null ? card.getCardUid() : null)
                .cardStatus(card != null ? card.getStatus() : null)
                .ticketId(getTicketId(ticket))
                .ticketUsageStatus(ticket != null ? ticket.getUsageStatus() : null)
                .entitlementId(null)
                .entitlementStatus(null)
                .qrId(transaction.getQrId())
                .qrPayloadHash(transaction.getQrPayloadHash())
                .tapType(transaction.getTapType())
                .journeyRef(transaction.getJourneyRef())
                .occurredAt(transaction.getOccurredAt())
                .receivedAt(transaction.getReceivedAt())
                .decision(transaction.getDecision())
                .reason(transaction.getReason())
                .syncStatus(transaction.getSyncStatus())
                .ticketProcessingStatus(transaction.getTicketProcessingStatus())
                .batchId(transaction.getBatchId())
                .rawEventRef(transaction.getRawEventRef())
                .rawEventAvailable(transaction.getRawEventRef() != null)
                .ticketUsageResultAvailable(transaction.getJourneyRef() != null
                        || transaction.getTicketProcessingStatus() != null)
                .auditAvailable(false)
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }

    private String getCardId(Card card) {
        return card != null ? card.getId() : null;
    }

    private String getTicketId(Ticket ticket) {
        return ticket != null ? ticket.getId() : null;
    }

    public record TransactionEvaluation(String decision, String reason, String qrId, DynamicQrSession session,
                                        Card card, Ticket ticket) {
    }
}