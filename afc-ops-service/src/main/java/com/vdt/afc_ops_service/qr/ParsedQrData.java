package com.vdt.afc_ops_service.qr;

public record ParsedQrData(
        String ticketId,
        long exp,
        boolean expired
) {}