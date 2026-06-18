package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionDetailResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionListItemResponse;
import com.vdt.afc_ops_service.service.ITransactionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/transaction")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionController {

    ITransactionService transactionService;

    @PostMapping("/submit-tap-event")
    public ApiResponse<SubmitTransactionResponse> submitTransaction(
            @RequestHeader("X-Device-Code") String deviceCode,
            @RequestHeader("X-Device-Secret") String deviceSecret,
            @Valid @RequestBody SubmitTransactionRequest request
    ) {
        return ApiResponse.<SubmitTransactionResponse>builder()
                .result(transactionService.submit(deviceCode, deviceSecret, request))
                .build();
    }

    @GetMapping("/search-transactions")
    public ApiResponse<PageResponse<TransactionListItemResponse>> searchTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) Long stationId,
            @RequestParam(required = false) Long deviceId,
            @RequestParam(required = false) String cardId,
            @RequestParam(required = false) String ticketId,
            @RequestParam(required = false) String entitlementId,
            @RequestParam(required = false) String tapType,
            @RequestParam(required = false) String decision,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String syncStatus,
            @RequestParam(required = false) String ticketProcessingStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<TransactionListItemResponse>>builder()
                .result(transactionService.searchTransactions(from, to, routeId, stationId, deviceId,
                        cardId, ticketId, entitlementId, tapType, decision, reason, syncStatus,
                        ticketProcessingStatus, page, size))
                .build();
    }

    @GetMapping("/get-transaction-detail")
    public ApiResponse<TransactionDetailResponse> getTransactionDetail(@RequestParam String transactionId) {
        return ApiResponse.<TransactionDetailResponse>builder()
                .result(transactionService.getTransactionDetail(transactionId))
                .build();
    }
}
