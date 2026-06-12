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
public class Level5CardPayload {

    private String cardId;
    private String cardUid;
    private String cardType;
    private String status;
    private String statusReason;
}
