package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionDetailResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionListItemResponse;

import java.time.LocalDateTime;

public interface ITransactionService {
    SubmitTransactionResponse submit(SubmitTransactionRequest request);

    PageResponse<TransactionListItemResponse> searchTransactions(LocalDateTime from, LocalDateTime to,
                                                                 Long routeId, Long stationId, Long deviceId,
                                                                 String cardId, String ticketId, String entitlementId,
                                                                 String tapType, String decision, String reason,
                                                                 String syncStatus, String ticketProcessingStatus,
                                                                 int page, int size);

    TransactionDetailResponse getTransactionDetail(String transactionId);
}
