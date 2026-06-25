package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedDeviceDirection;
import com.vdt.afc_ops_service.constant.PredefinedDeviceStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransactionDecision;
import com.vdt.afc_ops_service.constant.PredefinedTransactionReason;
import com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionDetailResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionListItemResponse;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.entity.Transaction;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.mapper.TransactionMapper;
import com.vdt.afc_ops_service.mapper.TransactionMapper.TransactionEvaluation;
import com.vdt.afc_ops_service.qr.DynamicQrSession;
import com.vdt.afc_ops_service.qr.DynamicQrSessionStore;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.TicketRepository;
import com.vdt.afc_ops_service.repository.TransactionRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.ITransactionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    static final int MAX_PAGE_SIZE = 100;
    static final LocalDateTime MIN_TRANSACTION_TIME = LocalDateTime.of(1900, 1, 1, 0, 0);
    static final LocalDateTime MAX_TRANSACTION_TIME = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    DeviceRepository deviceRepository;
    CardRepository cardRepository;
    TicketRepository ticketRepository;
    TransactionRepository transactionRepository;
    DynamicQrSessionStore dynamicQrSessionStore;
    TransactionMapper transactionMapper;
    SecurityUtils securityUtils;

    @Override
    @Transactional
    public SubmitTransactionResponse submit(String deviceCode, String deviceSecret, SubmitTransactionRequest request) {
        LocalDateTime now = LocalDateTime.now();
        String normalizedDeviceCode = SearchFilterUtil.normalize(deviceCode);
        Device device = deviceRepository.findByDeviceCode(normalizedDeviceCode)
                .orElseThrow(() -> new AppException(ErrorCode.DEVICE_NOT_FOUND));
        if (!Objects.equals(SearchFilterUtil.normalize(deviceSecret), device.getDeviceSecret())) {
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<TransactionListItemResponse> searchTransactions(LocalDateTime from, LocalDateTime to,
                                                                        Long routeId, Long stationId, Long deviceId,
                                                                        String cardId, String ticketId,
                                                                        String entitlementId, String tapType,
                                                                        String decision, String reason,
                                                                        String syncStatus,
                                                                        String ticketProcessingStatus,
                                                                        int page, int size) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        validateSearchParams(from, to, routeId, stationId, deviceId, page, size);
        String normalizedCardId = SearchFilterUtil.normalize(cardId);
        String normalizedTicketId = SearchFilterUtil.normalize(ticketId);
        String normalizedEntitlementId = SearchFilterUtil.normalize(entitlementId);
        String normalizedTapType = SearchFilterUtil.normalizeUppercase(tapType);
        String normalizedDecision = SearchFilterUtil.normalizeUppercase(decision);
        String normalizedReason = SearchFilterUtil.normalizeUppercase(reason);
        String normalizedSyncStatus = SearchFilterUtil.normalizeUppercase(syncStatus);
        String normalizedTicketProcessingStatus = SearchFilterUtil.normalizeUppercase(ticketProcessingStatus);
        LocalDateTime queryFrom = from == null ? MIN_TRANSACTION_TIME : from;
        LocalDateTime queryTo = to == null ? MAX_TRANSACTION_TIME : to;

        Page<Transaction> transactions = transactionRepository.searchTransactions(
                operator.getId(),
                queryFrom,
                queryTo,
                routeId,
                stationId,
                deviceId,
                normalizedCardId,
                normalizedTicketId,
                normalizedEntitlementId,
                normalizedTapType,
                normalizedDecision,
                normalizedReason,
                normalizedSyncStatus,
                normalizedTicketProcessingStatus,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "occurredAt"))
        );

        return PageResponse.<TransactionListItemResponse>builder()
                .items(transactions.getContent().stream()
                        .map(transactionMapper::toListItemResponse)
                        .toList())
                .page(transactions.getNumber())
                .size(transactions.getSize())
                .totalElements(transactions.getTotalElements())
                .totalPages(transactions.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDetailResponse getTransactionDetail(String transactionId) {
        String normalizedTransactionId = SearchFilterUtil.normalize(transactionId);
        validateTransactionId(normalizedTransactionId);
        Operator operator = securityUtils.getRequiredCurrentOperator();

        Transaction transaction = transactionRepository
                .findDetailByIdAndOperatorId(normalizedTransactionId, operator.getId())
                .orElseGet(() -> {
                    if (transactionRepository.existsById(normalizedTransactionId)) {
                        throw new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
                    }
                    throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
                });

        return transactionMapper.toDetailResponse(transaction);
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
        // Entitlement merged into Ticket — lookup as monthly pass via ticketId
        Ticket ticket = ticketRepository.findByIdAndCardId(session.entitlementId(), card.getId()).orElse(null);
        if (ticket == null) {
            return transactionMapper.denyCard(qrId, session, card, PredefinedTransactionReason.ENTITLEMENT_INACTIVE);
        }
        if (PredefinedLevel5BusinessSync.EXPIRED.equals(ticket.getUsageStatus())
                || now.isBefore(ticket.getValidFrom())
                || now.isAfter(ticket.getValidTo())) {
            return transactionMapper.denyTicket(qrId, session, card, ticket,
                    PredefinedTransactionReason.ENTITLEMENT_EXPIRED);
        }
        if (!PredefinedLevel5BusinessSync.ACTIVE.equals(ticket.getUsageStatus())) {
            return transactionMapper.denyTicket(qrId, session, card, ticket,
                    PredefinedTransactionReason.ENTITLEMENT_INACTIVE);
        }
        return transactionMapper.allowTicket(qrId, session, card, ticket);
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

    private void validateSearchParams(LocalDateTime from, LocalDateTime to, Long routeId, Long stationId,
                                      Long deviceId, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_TIME_RANGE);
        }
        validateOptionalPositiveId(routeId, ErrorCode.INVALID_ROUTE_ID);
        validateOptionalPositiveId(stationId, ErrorCode.INVALID_STATION_ID);
        validateOptionalPositiveId(deviceId, ErrorCode.INVALID_DEVICE_ID);
    }

    private void validateOptionalPositiveId(Long id, ErrorCode errorCode) {
        if (id != null && id <= 0) {
            throw new AppException(errorCode);
        }
    }

    private void validateTransactionId(String transactionId) {
        if (transactionId == null || transactionId.length() > 36) {
            throw new AppException(ErrorCode.INVALID_TRANSACTION_ID);
        }
    }

}
