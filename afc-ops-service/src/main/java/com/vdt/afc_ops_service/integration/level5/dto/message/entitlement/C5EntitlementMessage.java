package com.vdt.afc_ops_service.integration.level5.dto.message.entitlement;

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
public class C5EntitlementMessage {

    private UUID ticketId;
    private String type;
    private String mode;
    private String scope;
    private UUID cardId;
    private UUID userId;
    private String fromStationCode;
    private String toStationCode;
    private BigDecimal fareAmount;
    private String status;
    private LocalDate validFrom;
    private LocalDate validTo;
    private Instant issuedAt;
}
