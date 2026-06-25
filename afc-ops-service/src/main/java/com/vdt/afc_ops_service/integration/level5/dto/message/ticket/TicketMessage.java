package com.vdt.afc_ops_service.integration.level5.dto.message.ticket;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import java.util.UUID;

public record TicketMessage(
    UUID ticketId,
    String type,
    String mode,
    String scope,
    UUID cardId,
    UUID userId,
    String fromStationCode,
    String toStationCode,
    BigDecimal fareAmount,
    LocalDate validFrom,
    LocalDate validTo,
    Instant issuedAt
) {}
