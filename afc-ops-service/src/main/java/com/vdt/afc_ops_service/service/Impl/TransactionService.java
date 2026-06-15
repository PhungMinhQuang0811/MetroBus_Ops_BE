package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedDeviceDirection;
import com.vdt.afc_ops_service.constant.PredefinedDeviceStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransactionDecision;
import com.vdt.afc_ops_service.constant.PredefinedTransactionReason;
import com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest;
import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Entitlement;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.entity.Transaction;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.mapper.TransactionMapper;
import com.vdt.afc_ops_service.mapper.TransactionMapper.TransactionEvaluation;
import com.vdt.afc_ops_service.qr.DynamicQrSession;
import com.vdt.afc_ops_service.qr.DynamicQrSessionStore;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.EntitlementRepository;
import com.vdt.afc_ops_service.repository.TicketRepository;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import com.vdt.afc_ops_service.service.ITransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionService implements ITransactionService {

    static final String TAP_TYPE_TAP_IN = "TAP_IN";
    static final String TAP_TYPE_TAP_OUT = "TAP_OUT";

    DeviceRepository deviceRepository;
    CardRepository cardRepository;
    TicketRepository ticketRepository;
    EntitlementRepository entitlementRepository;
    TransactionRepository transactionRepository;
    DynamicQrSessionStore dynamicQrSessionStore;
    TransactionMapper transactionMapper;

    @Override
    @Transactional
    public SubmitTransactionResponse submit(SubmitTransactionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String deviceCode = SearchFilterUtil.normalize(request.getDeviceCode());
        Device device = deviceRepository.findByDeviceCode(deviceCode)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));
        if (!Objects.equals(SearchFilterUtil.normalize(request.getDeviceSecret()), device.getDeviceSecret())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String direction = normalizeUppercase(device.getDirection());
        TransactionEvaluation evaluation = evaluate(request, device, direction, now);
        Transaction transaction = saveTransaction(request, device, direction, evaluation, now);

        if (PredefinedTransactionDecision.OPEN_GATE.equals(evaluation.decision()) && evaluation.qrId() != null) {
            dynamicQrSessionStore.markUsed(evaluation.qrId(), evaluation.session());
        }

        return transactionMapper.toResponse(transaction, evaluation, now);
    }

    private TransactionEvaluation evaluate(SubmitTransactionRequest request, Device device, String direction, LocalDateTime now) {
        if (!PredefinedDeviceStatus.ACTIVE.equals(device.getStatus())) {
            return transactionMapper.deny(PredefinedTransactionReason.DEVICE_DISABLED);
        }
        if (!isValidTapDirection(direction)) {
            return transactionMapper.deny(PredefinedTransactionReason.INVALID_DIRECTION);
        }

        String qrPayload = SearchFilterUtil.normalize(request.getQrPayload());
        String qrId = dynamicQrSessionStore.parseQrId(qrPayload);
        if (qrId == null) {
            return transactionMapper.deny(PredefinedTransactionReason.QR_INVALID);
        }

        DynamicQrSession session = dynamicQrSessionStore.find(qrId);
        if (session == null) {
            return transactionMapper.denyQr(qrId, null, PredefinedTransactionReason.QR_EXPIRED);
        }
        if (session.used()) {
            return transactionMapper.denyQr(qrId, session, PredefinedTransactionReason.QR_REPLAYED);
        }
        if (session.expiresAt() != null && session.expiresAt() < toEpochSecond(now)) {
            return transactionMapper.denyQr(qrId, session, PredefinedTransactionReason.QR_EXPIRED);
        }
        if (session.cardId() == null) {
            return transactionMapper.denyQr(qrId, session, PredefinedTransactionReason.UNKNOWN_MEDIA);
        }

        Card card = cardRepository.findById(session.cardId()).orElse(null);
        if (card == null) {
            return transactionMapper.denyQr(qrId, session, PredefinedTransactionReason.UNKNOWN_MEDIA);
        }
        String cardReason = validateCard(card);
        if (cardReason != null) {
            return transactionMapper.denyCard(qrId, session, card, cardReason);
        }

        if (session.ticketId() != null && session.entitlementId() != null) {
            return transactionMapper.denyCard(qrId, session, card,
                    PredefinedTransactionReason.ACTIVE_PRODUCT_CONFLICT);
        }
        if (session.ticketId() != null) {
            return evaluateTicket(qrId, session, card, direction, now);
        }
        if (session.entitlementId() != null) {
            return evaluateEntitlement(qrId, session, card, now);
        }
        return transactionMapper.denyCard(qrId, session, card, PredefinedTransactionReason.ACTIVE_PRODUCT_NOT_FOUND);
    }

    private TransactionEvaluation evaluateTicket(String qrId, DynamicQrSession session, Card card, String direction, LocalDateTime now) {
        Ticket ticket = ticketRepository.findByIdAndCardId(session.ticketId(), card.getId()).orElse(null);
        if (ticket == null) {
            return transactionMapper.denyCard(qrId, session, card, PredefinedTransactionReason.TICKET_INVALID);
        }
        String validityReason = validateTicketValidity(ticket, now);
        if (validityReason != null) {
            return transactionMapper.denyTicket(qrId, session, card, ticket, validityReason);
        }
        if (PredefinedLevel5BusinessSync.UNUSED.equals(ticket.getUsageStatus())) {
            if (!PredefinedDeviceDirection.ENTRY.equals(direction)) {
                return transactionMapper.denyTicket(qrId, session, card, ticket,
                        PredefinedTransactionReason.TICKET_INVALID);
            }
            ticket.setUsageStatus(PredefinedLevel5BusinessSync.IN_USE);
            ticket.setFirstTapAt(now);
            ticketRepository.save(ticket);
            return transactionMapper.allowTicket(qrId, session, card, ticket);
        }
        if (PredefinedLevel5BusinessSync.IN_USE.equals(ticket.getUsageStatus())) {
            if (!PredefinedDeviceDirection.EXIT.equals(direction)) {
                return transactionMapper.denyTicket(qrId, session, card, ticket,
                        PredefinedTransactionReason.TICKET_INVALID);
            }
            ticket.setUsageStatus(PredefinedLevel5BusinessSync.USED);
            ticket.setUsedAt(now);
            ticketRepository.save(ticket);
            return transactionMapper.allowTicket(qrId, session, card, ticket);
        }
        if (PredefinedLevel5BusinessSync.USED.equals(ticket.getUsageStatus())) {
            return transactionMapper.denyTicket(qrId, session, card, ticket,
                    PredefinedTransactionReason.TICKET_ALREADY_USED);
        }
        if (PredefinedLevel5BusinessSync.EXPIRED.equals(ticket.getUsageStatus())) {
            return transactionMapper.denyTicket(qrId, session, card, ticket,
                    PredefinedTransactionReason.TICKET_EXPIRED);
        }
        return transactionMapper.denyTicket(qrId, session, card, ticket, PredefinedTransactionReason.TICKET_INVALID);
    }

    private TransactionEvaluation evaluateEntitlement(String qrId, DynamicQrSession session, Card card, LocalDateTime now) {
        Entitlement entitlement = entitlementRepository.findByIdAndCardId(session.entitlementId(), card.getId()).orElse(null);
        if (entitlement == null) {
            return transactionMapper.denyCard(qrId, session, card, PredefinedTransactionReason.ENTITLEMENT_INACTIVE);
        }
        if (PredefinedLevel5BusinessSync.EXPIRED.equals(entitlement.getStatus())
                || now.isBefore(entitlement.getValidFrom())
                || now.isAfter(entitlement.getValidTo())) {
            return transactionMapper.denyEntitlement(qrId, session, card, entitlement,
                    PredefinedTransactionReason.ENTITLEMENT_EXPIRED);
        }
        if (!PredefinedLevel5BusinessSync.ACTIVE.equals(entitlement.getStatus())) {
            return transactionMapper.denyEntitlement(qrId, session, card, entitlement,
                    PredefinedTransactionReason.ENTITLEMENT_INACTIVE);
        }
        return transactionMapper.allowEntitlement(qrId, session, card, entitlement);
    }

    private String validateCard(Card card) {
        if (PredefinedLevel5BusinessSync.BLACKLISTED.equals(card.getStatus())) {
            return PredefinedTransactionReason.MEDIA_BLACKLISTED;
        }
        if (PredefinedLevel5BusinessSync.CANCELLED.equals(card.getStatus())) {
            return PredefinedTransactionReason.CARD_CANCELLED;
        }
        if (!PredefinedLevel5BusinessSync.ACTIVE.equals(card.getStatus())) {
            return PredefinedTransactionReason.CARD_INACTIVE;
        }
        return null;
    }

    private String validateTicketValidity(Ticket ticket, LocalDateTime now) {
        if (PredefinedLevel5BusinessSync.EXPIRED.equals(ticket.getUsageStatus())
                || now.isBefore(ticket.getValidFrom())
                || now.isAfter(ticket.getValidTo())) {
            return PredefinedTransactionReason.TICKET_EXPIRED;
        }
        return null;
    }

    private Transaction saveTransaction(SubmitTransactionRequest request, Device device, String direction,
                                        TransactionEvaluation evaluation, LocalDateTime now) {
        String transactionId = UUID.randomUUID().toString();
        Transaction transaction = transactionMapper.toTransaction(
                request,
                device,
                direction,
                resolveTapType(direction),
                evaluation,
                transactionId,
                now
        );
        return transactionRepository.save(transaction);
    }

    private boolean isValidTapDirection(String direction) {
        return PredefinedDeviceDirection.ENTRY.equals(direction) || PredefinedDeviceDirection.EXIT.equals(direction);
    }

    private String resolveTapType(String direction) {
        return PredefinedDeviceDirection.EXIT.equals(direction) ? TAP_TYPE_TAP_OUT : TAP_TYPE_TAP_IN;
    }

    private String normalizeUppercase(String value) {
        return SearchFilterUtil.normalizeUppercase(value);
    }

    private long toEpochSecond(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

}
