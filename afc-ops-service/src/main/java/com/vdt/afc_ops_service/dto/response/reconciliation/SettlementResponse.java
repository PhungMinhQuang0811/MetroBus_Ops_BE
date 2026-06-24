package com.vdt.afc_ops_service.dto.response.reconciliation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SettlementResponse {
    Long id;
    String settlementId;
    String period;
    String operatorCode;
    BigDecimal allocatedAmount;
    BigDecimal totalKm;
    Integer totalTrips;
    BigDecimal kmRatio;
    LocalDateTime createdAt;
}