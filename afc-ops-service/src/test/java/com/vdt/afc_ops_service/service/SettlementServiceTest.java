package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.reconciliation.SettlementResponse;
import com.vdt.afc_ops_service.entity.OperatorSettlement;
import com.vdt.afc_ops_service.integration.level5.dto.message.settlement.CompanyShareMessage;
import com.vdt.afc_ops_service.repository.OperatorSettlementRepository;
import com.vdt.afc_ops_service.service.Impl.SettlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettlementServiceTest {

    @Mock
    OperatorSettlementRepository settlementRepository;

    @InjectMocks
    SettlementService settlementService;

    // -------------------------------------------------------
    // processSettlement
    // -------------------------------------------------------

    @Test
    void processSettlement_shouldSave_whenValidShare() {
        UUID settlementId = UUID.randomUUID();
        String period = "2026-06-24";
        CompanyShareMessage share = new CompanyShareMessage(
                UUID.randomUUID(), "HCMC-METRO",
                new BigDecimal("12545000.00"), new BigDecimal("1420.50"),
                2450, new BigDecimal("0.3542")
        );

        when(settlementRepository.findBySettlementIdAndOperatorCode(settlementId.toString(), "HCMC-METRO"))
                .thenReturn(Optional.empty());

        settlementService.processSettlement(settlementId, period, share);

        verify(settlementRepository).save(any(OperatorSettlement.class));
    }

    @Test
    void processSettlement_shouldSkip_whenAlreadyProcessed() {
        UUID settlementId = UUID.randomUUID();
        String period = "2026-06-24";
        CompanyShareMessage share = new CompanyShareMessage(
                UUID.randomUUID(), "HCMC-METRO",
                BigDecimal.ONE, BigDecimal.ONE, 1, BigDecimal.ONE
        );

        when(settlementRepository.findBySettlementIdAndOperatorCode(settlementId.toString(), "HCMC-METRO"))
                .thenReturn(Optional.of(new OperatorSettlement()));

        settlementService.processSettlement(settlementId, period, share);

        verify(settlementRepository, never()).save(any(OperatorSettlement.class));
    }

    @Test
    void processSettlement_shouldSkip_whenNullOperatorCode() {
        UUID settlementId = UUID.randomUUID();
        CompanyShareMessage share = new CompanyShareMessage(
                UUID.randomUUID(), null,
                BigDecimal.ONE, BigDecimal.ONE, 1, BigDecimal.ONE
        );

        settlementService.processSettlement(settlementId, "2026-06-24", share);

        verify(settlementRepository, never()).save(any(OperatorSettlement.class));
    }

    @Test
    void processSettlement_shouldSkip_whenBlankOperatorCode() {
        UUID settlementId = UUID.randomUUID();
        CompanyShareMessage share = new CompanyShareMessage(
                UUID.randomUUID(), "",
                BigDecimal.ONE, BigDecimal.ONE, 1, BigDecimal.ONE
        );

        settlementService.processSettlement(settlementId, "2026-06-24", share);

        verify(settlementRepository, never()).save(any(OperatorSettlement.class));
    }

    // -------------------------------------------------------
    // listSettlements
    // -------------------------------------------------------

    @Test
    void listSettlements_shouldReturnPage_whenValidParams() {
        OperatorSettlement settlement = OperatorSettlement.builder()
                .id(1L)
                .settlementId(UUID.randomUUID().toString())
                .period("2026-06-24")
                .operatorCode("HCMC-METRO")
                .allocatedAmount(new BigDecimal("12545000.00"))
                .totalKm(new BigDecimal("1420.50"))
                .totalTrips(2450)
                .kmRatio(new BigDecimal("0.3542"))
                .build();

        when(settlementRepository.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(settlement), PageRequest.of(0, 20), 1));

        PageResponse<SettlementResponse> result = settlementService.listSettlements(0, 20);

        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1, result.getTotalElements());
        assertEquals(0, result.getPage());
        assertEquals(20, result.getSize());

        SettlementResponse item = result.getItems().get(0);
        assertEquals("HCMC-METRO", item.getOperatorCode());
        assertEquals(new BigDecimal("12545000.00"), item.getAllocatedAmount());
    }

    @Test
    void listSettlements_shouldReturnEmpty_whenNoData() {
        when(settlementRepository.findAll(PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        PageResponse<SettlementResponse> result = settlementService.listSettlements(0, 20);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void listSettlements_shouldThrow_whenInvalidPage() {
        AppException exception = assertThrows(AppException.class,
                () -> settlementService.listSettlements(-1, 20));
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST.getCode(), exception.getErrorCode().getCode());
    }

    @Test
    void listSettlements_shouldThrow_whenInvalidSize() {
        AppException exception = assertThrows(AppException.class,
                () -> settlementService.listSettlements(0, 200));
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST.getCode(), exception.getErrorCode().getCode());
    }
}