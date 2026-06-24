package com.vdt.afc_ops_service.integration.level5.dto.message.settlement;

import java.util.List;
import java.util.UUID;

public record SettlementConfirmedEvent(
        UUID settlementId,
        String period,
        List<CompanyShareMessage> shares
) {}