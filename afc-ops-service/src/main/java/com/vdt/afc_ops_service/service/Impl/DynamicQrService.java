package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.dto.request.qr.GenerateDynamicQrRequest;
import com.vdt.afc_ops_service.dto.response.qr.DynamicQrResponse;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.mapper.DynamicQrMapper;
import com.vdt.afc_ops_service.qr.DynamicQrSessionStore;
import com.vdt.afc_ops_service.repository.TicketRepository;
import com.vdt.afc_ops_service.service.IDynamicQrService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DynamicQrService implements IDynamicQrService {

    TicketRepository ticketRepository;
    DynamicQrSessionStore dynamicQrSessionStore;
    DynamicQrMapper dynamicQrMapper;

    @NonFinal
    @Value("${app.dynamic-qr.ttl-seconds}")
    int ttlSeconds;

    @NonFinal
    @Value("${app.security.qr-hmac-secret}")
    String qrHmacSecret;

    @Override
    @Transactional(readOnly = true)
    public DynamicQrResponse generate(GenerateDynamicQrRequest request) {
        String ticketId = SearchFilterUtil.normalize(request.getTicketId());
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(ErrorCode.TICKET_NOT_FOUND));

        validateTicket(ticket);

        // If ticket is linked to a physical card, validate card status too
        if (ticket.getCard() != null) {
            validateCard(ticket.getCard());
        }

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(ttlSeconds);

        String qrId = UUID.randomUUID().toString();
        long exp = toEpochSecond(expiresAt);
        String qrPayload = dynamicQrSessionStore.buildHmacSignedPayload(ticket.getId(), exp, qrHmacSecret);

        String cardId = ticket.getCard() != null ? ticket.getCard().getId() : null;
        dynamicQrSessionStore.create(qrId, dynamicQrMapper.toSession(
                cardId,
                "SINGLE_TRIP".equals(ticket.getType()) ? ticket.getId() : null,
                "MONTHLY_PASS".equals(ticket.getType()) ? ticket.getId() : null,
                exp
        ), ttlSeconds);

        return dynamicQrMapper.toResponse(qrId, qrPayload, expiresAt, ttlSeconds);
    }

    private void validateTicket(Ticket ticket) {
        if (PredefinedLevel5BusinessSync.EXPIRED.equals(ticket.getUsageStatus())) {
            throw new AppException(ErrorCode.TICKET_EXPIRED);
        }
        if (PredefinedLevel5BusinessSync.USED.equals(ticket.getUsageStatus())) {
            throw new AppException(ErrorCode.TICKET_ALREADY_USED);
        }
        if (PredefinedLevel5BusinessSync.CANCELLED.equals(ticket.getUsageStatus())) {
            throw new AppException(ErrorCode.TICKET_INVALID);
        }
        if (!PredefinedLevel5BusinessSync.ACTIVE.equals(ticket.getUsageStatus())
                && !PredefinedLevel5BusinessSync.UNUSED.equals(ticket.getUsageStatus())
                && !PredefinedLevel5BusinessSync.IN_USE.equals(ticket.getUsageStatus())) {
            throw new AppException(ErrorCode.TICKET_INVALID);
        }
    }

    private void validateCard(Card card) {
        if (PredefinedLevel5BusinessSync.BLACKLISTED.equals(card.getStatus())) {
            throw new AppException(ErrorCode.MEDIA_BLACKLISTED);
        }
        if (PredefinedLevel5BusinessSync.CANCELLED.equals(card.getStatus())) {
            throw new AppException(ErrorCode.CARD_CANCELLED);
        }
        if (!PredefinedLevel5BusinessSync.ACTIVE.equals(card.getStatus())) {
            throw new AppException(ErrorCode.CARD_INACTIVE);
        }
    }

    private long toEpochSecond(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }
}