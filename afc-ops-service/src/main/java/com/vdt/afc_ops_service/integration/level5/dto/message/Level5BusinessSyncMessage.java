package com.vdt.afc_ops_service.integration.level5.dto.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Level5BusinessSyncMessage {

    private String syncType;
    private String sourceSystem;
    private String correlationId;
    private List<Level5BusinessSyncItemMessage> items;
}
