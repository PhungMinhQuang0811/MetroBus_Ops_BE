package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.dto.response.qr.DynamicQrResponse;
import com.vdt.afc_ops_service.qr.DynamicQrSession;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DynamicQrMapper {

    public DynamicQrSession toSession(String cardId, String ticketId, String entitlementId, long expiresAt) {
        return new DynamicQrSession(cardId, ticketId, entitlementId, expiresAt, false);
    }

    public DynamicQrResponse toResponse(String qrId, String qrPayload, LocalDateTime expiresAt, int refreshAfterSeconds) {
        return DynamicQrResponse.builder()
                .qrId(qrId)
                .qrPayload(qrPayload)
                .expiresAt(expiresAt)
                .refreshAfterSeconds(refreshAfterSeconds)
                .build();
    }
}
