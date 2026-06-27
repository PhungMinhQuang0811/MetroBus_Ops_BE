package com.vdt.afc_ops_service.dto.request.transaction;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitBatchRequest {

    @NotNull
    @Size(min = 1, max = 500)
    @Valid
    List<BatchTransactionItem> transactions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class BatchTransactionItem {
        @NotNull
        String qrPayload;
        String tapType;
        @NotNull
        LocalDateTime occurredAt;
    }
}