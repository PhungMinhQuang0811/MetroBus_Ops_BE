package com.vdt.afc_ops_service.integration.level5.dto.response;

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
public class Level5BusinessSyncResult {

    private String syncType;
    private String correlationId;
    private int processedCount;
    private Long createdCount;
    private Long updatedCount;
    private Long ignoredCount;
    private Long rejectedCount;
    private List<Level5BusinessSyncItemResult> items;
}
