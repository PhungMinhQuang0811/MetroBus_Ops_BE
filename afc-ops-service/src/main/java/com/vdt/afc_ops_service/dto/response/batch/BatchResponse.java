package com.vdt.afc_ops_service.dto.response.batch;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BatchResponse {
    String id;
    String batchCode;
    LocalDateTime fromTime;
    LocalDateTime toTime;
    int transactionCount;
    String status;
    LocalDateTime submittedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
