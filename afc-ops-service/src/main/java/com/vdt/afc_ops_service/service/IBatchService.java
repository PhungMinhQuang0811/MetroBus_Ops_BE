package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.request.batch.CreateBatchRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.batch.BatchResponse;
import com.vdt.afc_ops_service.entity.Operator;

import java.time.LocalDateTime;

public interface IBatchService {

    BatchResponse createBatch(CreateBatchRequest request);

    BatchResponse createBatchForOperator(Operator operator, CreateBatchRequest request);

    PageResponse<BatchResponse> listBatches(String status, LocalDateTime from, LocalDateTime to,
                                            int page, int size);

    BatchResponse submitBatchToLevel5(String batchId);
}
