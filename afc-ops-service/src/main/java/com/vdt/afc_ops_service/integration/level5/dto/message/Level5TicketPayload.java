package com.vdt.afc_ops_service.integration.level5.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Level5TicketPayload {

    private String ticketId;
    private String cardId;
    private String ticketType;
    private String scope;
    private String mode;
    private String operatorRef;
    private String routeRef;
    private String fromStationRef;
    private String toStationRef;
    private BigDecimal price;
    private String usageStatus;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private LocalDateTime firstTapAt;
    private LocalDateTime usedAt;
}