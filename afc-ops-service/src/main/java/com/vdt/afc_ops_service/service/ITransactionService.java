package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest;
import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;

public interface ITransactionService {
    SubmitTransactionResponse submit(SubmitTransactionRequest request);
}
