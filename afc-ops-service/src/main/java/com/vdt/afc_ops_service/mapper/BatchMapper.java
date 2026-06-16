package com.vdt.afc_ops_service.mapper;

import com.vdt.afc_ops_service.dto.response.batch.BatchResponse;
import com.vdt.afc_ops_service.entity.Batch;
import org.springframework.stereotype.Component;

@Component
public class BatchMapper {

    public BatchResponse toResponse(Batch batch) {
        return BatchResponse.builder()
                .id(batch.getId())
                .batchCode(batch.getBatchCode())
                .fromTime(batch.getFromTime())
                .toTime(batch.getToTime())
                .transactionCount(batch.getTransactionCount())
                .status(batch.getStatus())
                .submittedAt(batch.getSubmittedAt())
                .createdAt(batch.getCreatedAt())
                .updatedAt(batch.getUpdatedAt())
                .build();
    }
}
