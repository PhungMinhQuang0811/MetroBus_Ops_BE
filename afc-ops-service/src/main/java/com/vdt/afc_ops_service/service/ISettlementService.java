package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.reconciliation.SettlementResponse;
import com.vdt.afc_ops_service.integration.level5.dto.message.settlement.CompanyShareMessage;

import java.util.UUID;

public interface ISettlementService {

    void processSettlement(UUID settlementId, String period, CompanyShareMessage share);

    PageResponse<SettlementResponse> listSettlements(int page, int size);
}