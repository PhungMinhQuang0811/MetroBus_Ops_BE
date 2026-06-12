package com.vdt.afc_ops_service.integration.level5.dto.message.ticket;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class C5TicketSyncMessage {

    private UUID id;
    private UUID cardId;
    private UUID userId;
    private String type;
    private BigDecimal price;
    private UUID fareRuleId;
    private UUID discountId;
    private UUID fromStationId;
    private UUID toStationId;
    private String mode;
    private LocalDate validFrom;
    private LocalDate validTo;
    private String status;
    private String scope;
    private Instant purchasedAt;
    private Instant usedAt;
}
