package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest;
import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;
import com.vdt.afc_ops_service.service.ITransactionService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TransactionController {

    ITransactionService transactionService;

    @PostMapping("/submit-tap-event")
    public ApiResponse<SubmitTransactionResponse> submitTransaction(@Valid @RequestBody SubmitTransactionRequest request) {
        return ApiResponse.<SubmitTransactionResponse>builder()
                .result(transactionService.submit(request))
                .build();
    }
}
