package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.reconciliation.SettlementResponse;
import com.vdt.afc_ops_service.service.ISettlementService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reconciliation")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReconciliationController {

    ISettlementService settlementService;

    @GetMapping("/settlements")
    public ApiResponse<PageResponse<SettlementResponse>> listSettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<SettlementResponse>>builder()
                .result(settlementService.listSettlements(page, size))
                .build();
    }
}