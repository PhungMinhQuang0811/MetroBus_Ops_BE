package com.vdt.afc_ops_service.integration.level5.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Level5EntitlementPayload {

    private String entitlementId;
    private String cardId;
    private String fareProductCode;
    private String passPeriod;
    private String passScope;
    private String operatorRef;
    private String routeRef;
    private String transportType;
    private String passengerType;
    private String status;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
}
