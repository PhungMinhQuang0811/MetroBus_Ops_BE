package com.vdt.afc_ops_service.integration.level5.dto.message.card;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class C5CardStatusMessage {

    private UUID cardId;
    private String cardUid;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private UUID changedBy;
    private Instant occurredAt;
}
