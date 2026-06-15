package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.dto.request.qr.GenerateDynamicQrRequest;
import com.vdt.afc_ops_service.dto.response.qr.DynamicQrResponse;
import com.vdt.afc_ops_service.entity.Card;
import com.vdt.afc_ops_service.entity.Entitlement;
import com.vdt.afc_ops_service.entity.Ticket;
import com.vdt.afc_ops_service.integration.level5.constant.PredefinedLevel5BusinessSync;
import com.vdt.afc_ops_service.mapper.DynamicQrMapper;
import com.vdt.afc_ops_service.qr.DynamicQrSessionStore;
import com.vdt.afc_ops_service.repository.CardRepository;
import com.vdt.afc_ops_service.repository.EntitlementRepository;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DynamicQrService implements IDynamicQrService {

    CardRepository cardRepository;
    TicketRepository ticketRepository;
    EntitlementRepository entitlementRepository;
    DynamicQrSessionStore dynamicQrSessionStore;
    DynamicQrMapper dynamicQrMapper;

    @NonFinal
    @Value("${app.dynamic-qr.ttl-seconds}")
    int ttlSeconds;

    @Override
    @Transactional(readOnly = true)
    public DynamicQrResponse generate(GenerateDynamicQrRequest request) {
        String cardId = SearchFilterUtil.normalize(request.getCardId());
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(ErrorCode.CARD_NOT_FOUND));
        validateCard(card);

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(ttlSeconds);
        ActiveProduct activeProduct = resolveActiveProduct(cardId, issuedAt);

        String qrId = UUID.randomUUID().toString();
        long exp = toEpochSecond(expiresAt);
        String qrPayload = dynamicQrSessionStore.buildPayload(qrId);

        dynamicQrSessionStore.create(qrId, dynamicQrMapper.toSession(
                cardId,
                activeProduct.ticketId(),
                activeProduct.entitlementId(),
                exp
        ), ttlSeconds);

        return dynamicQrMapper.toResponse(qrId, qrPayload, expiresAt, ttlSeconds);
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

    private ActiveProduct resolveActiveProduct(String cardId, LocalDateTime now) {
        List<Ticket> tickets = ticketRepository.findAllByCardIdAndUsageStatusInAndValidToAfter(
                cardId,
                List.of(PredefinedLevel5BusinessSync.UNUSED, PredefinedLevel5BusinessSync.IN_USE),
                now
        );

        List<Entitlement> entitlements = entitlementRepository.findAllByCardIdAndStatusAndValidToAfter(
                cardId,
                PredefinedLevel5BusinessSync.ACTIVE,
                now
        );

        int activeProductCount = tickets.size() + entitlements.size();
        if (activeProductCount == 0) {
            throw new AppException(ErrorCode.ACTIVE_PRODUCT_NOT_FOUND);
        }
        if (activeProductCount > 1) {
            throw new AppException(ErrorCode.ACTIVE_PRODUCT_CONFLICT);
        }
        if (!tickets.isEmpty()) {
            return new ActiveProduct(tickets.get(0).getId(), null);
        }
        return new ActiveProduct(null, entitlements.get(0).getId());
    }

    private long toEpochSecond(LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toEpochSecond();
    }

    private record ActiveProduct(String ticketId, String entitlementId) {
    }
}
