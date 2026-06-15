package com.vdt.afc_ops_service.qr;

public record DynamicQrSession(
        String cardId,
        String ticketId,
        String entitlementId,
        Long expiresAt,
        boolean used
) {
}
