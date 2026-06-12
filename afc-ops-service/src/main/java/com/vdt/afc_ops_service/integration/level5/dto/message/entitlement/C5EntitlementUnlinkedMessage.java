package com.vdt.afc_ops_service.integration.level5.dto.message.entitlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class C5EntitlementUnlinkedMessage {

    private UUID ticketId;
    private UUID cardId;
}
