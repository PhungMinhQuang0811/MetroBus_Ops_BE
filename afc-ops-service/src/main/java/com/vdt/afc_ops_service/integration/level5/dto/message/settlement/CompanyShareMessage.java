package com.vdt.afc_ops_service.integration.level5.dto.message.settlement;

import java.math.BigDecimal;
import java.util.UUID;

public record CompanyShareMessage(
        UUID operatorId,
        String operatorCode,
        BigDecimal allocatedAmount,
        BigDecimal totalKm,
        Integer totalTrips,
        BigDecimal kmRatio
) {}