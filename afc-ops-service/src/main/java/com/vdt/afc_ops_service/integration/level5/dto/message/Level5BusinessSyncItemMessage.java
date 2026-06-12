package com.vdt.afc_ops_service.integration.level5.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Level5BusinessSyncItemMessage {

    private String externalId;
    private Long sourceVersion;
    private Level5CardPayload card;
    private Level5TicketPayload ticket;
    private Level5EntitlementPayload entitlement;
}
