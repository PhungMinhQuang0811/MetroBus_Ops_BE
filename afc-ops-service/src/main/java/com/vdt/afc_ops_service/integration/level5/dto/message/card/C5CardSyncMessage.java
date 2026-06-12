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
public class C5CardSyncMessage {

    private UUID id;
    private String cardUid;
    private String status;
    private String type;
    private Boolean supportsMetro;
    private Boolean supportsBus;
    private UUID issuedAtStationId;
    private UUID linkedUserId;
    private Instant activatedAt;
    private Instant linkedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
