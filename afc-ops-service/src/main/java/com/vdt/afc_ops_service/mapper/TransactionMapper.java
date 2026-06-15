package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.common.util.CryptoHashUtil;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedTransactionDecision;
import com.vdt.afc_ops_service.constant.PredefinedTransactionReason;
import com.vdt.afc_ops_service.constant.PredefinedTransactionStatus;
import com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest;
import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Entitlement;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.entity.Transaction;
import com.vdt.afc_ops_service.qr.DynamicQrSession;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TransactionMapper {

    public TransactionEvaluation deny(String reason) {
        return new TransactionEvaluation(PredefinedTransactionDecision.DENY, reason,
                null, null, null, null, null);
    }

    public TransactionEvaluation denyQr(String qrId, DynamicQrSession session, String reason) {
        return new TransactionEvaluation(PredefinedTransactionDecision.DENY, reason,
                qrId, session, null, null, null);
    }

    public TransactionEvaluation denyCard(String qrId, DynamicQrSession session, Card card, String reason) {
        return new TransactionEvaluation(PredefinedTransactionDecision.DENY, reason,
                qrId, session, card, null, null);
    }

    public TransactionEvaluation denyTicket(String qrId, DynamicQrSession session, Card card,
                                            Ticket ticket, String reason) {
        return new TransactionEvaluation(PredefinedTransactionDecision.DENY, reason,
                qrId, session, card, ticket, null);
    }

    public TransactionEvaluation denyEntitlement(String qrId, DynamicQrSession session, Card card,
                                                 Entitlement entitlement, String reason) {
        return new TransactionEvaluation(PredefinedTransactionDecision.DENY, reason,
                qrId, session, card, null, entitlement);
    }

    public TransactionEvaluation allowTicket(String qrId, DynamicQrSession session, Card card, Ticket ticket) {
        return new TransactionEvaluation(PredefinedTransactionDecision.OPEN_GATE, PredefinedTransactionReason.VALID,
                qrId, session, card, ticket, null);
    }

    public TransactionEvaluation allowEntitlement(String qrId, DynamicQrSession session, Card card,
                                                  Entitlement entitlement) {
        return new TransactionEvaluation(PredefinedTransactionDecision.OPEN_GATE, PredefinedTransactionReason.VALID,
                qrId, session, card, null, entitlement);
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
                .entitlement(evaluation.entitlement())
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

    public record TransactionEvaluation(String decision, String reason, String qrId, DynamicQrSession session,
                                        Card card, Ticket ticket, Entitlement entitlement) {
    }
}
