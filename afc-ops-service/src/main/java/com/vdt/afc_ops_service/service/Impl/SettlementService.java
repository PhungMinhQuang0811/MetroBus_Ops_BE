package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.reconciliation.SettlementResponse;
import com.vdt.afc_ops_service.entity.OperatorSettlement;
import com.vdt.afc_ops_service.integration.level5.dto.message.settlement.CompanyShareMessage;
import com.vdt.afc_ops_service.repository.OperatorSettlementRepository;
import com.vdt.afc_ops_service.service.ISettlementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettlementService implements ISettlementService {

    static final int MAX_PAGE_SIZE = 100;

    OperatorSettlementRepository settlementRepository;

    @Override
    @Transactional
    public void processSettlement(UUID settlementId, String period, CompanyShareMessage share) {
        String settlementIdStr = settlementId.toString();
        String operatorCode = share.operatorCode();

        if (operatorCode == null || operatorCode.isBlank()) {
            return;
        }

        // Idempotency: skip if already processed
        if (settlementRepository.findBySettlementIdAndOperatorCode(settlementIdStr, operatorCode).isPresent()) {
            return;
        }

        OperatorSettlement settlement = OperatorSettlement.builder()
                .settlementId(settlementIdStr)
                .period(period)
                .operatorCode(operatorCode)
                .allocatedAmount(share.allocatedAmount())
                .totalKm(share.totalKm())
                .totalTrips(share.totalTrips())
                .kmRatio(share.kmRatio())
                .build();

        settlementRepository.save(settlement);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<SettlementResponse> listSettlements(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }

        Page<OperatorSettlement> settlementPage = settlementRepository.findAll(
                PageRequest.of(page, size)
        );

        return PageResponse.<SettlementResponse>builder()
                .items(settlementPage.getContent().stream()
                        .map(this::toSettlementResponse)
                        .toList())
                .page(settlementPage.getNumber())
                .size(settlementPage.getSize())
                .totalElements(settlementPage.getTotalElements())
                .totalPages(settlementPage.getTotalPages())
                .build();
    }

    private SettlementResponse toSettlementResponse(OperatorSettlement settlement) {
        return SettlementResponse.builder()
                .id(settlement.getId())
                .settlementId(settlement.getSettlementId())
                .period(settlement.getPeriod())
                .operatorCode(settlement.getOperatorCode())
                .allocatedAmount(settlement.getAllocatedAmount())
                .totalKm(settlement.getTotalKm())
                .totalTrips(settlement.getTotalTrips())
                .kmRatio(settlement.getKmRatio())
                .createdAt(settlement.getCreatedAt())
                .build();
    }
}