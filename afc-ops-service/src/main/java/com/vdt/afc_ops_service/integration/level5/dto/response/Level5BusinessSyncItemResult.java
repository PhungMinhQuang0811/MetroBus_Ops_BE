package com.vdt.afc_ops_service.integration.level5.dto.response;

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
public class Level5BusinessSyncItemResult {

    private String externalId;
    private String result;
    private Long currentVersion;
    private String errorCode;
    private String message;
}
