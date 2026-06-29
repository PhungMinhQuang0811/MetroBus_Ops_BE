package com.vdt.afc_ops_service.integration.level5.service;

import com.vdt.afc_ops_service.entity.Batch;

public interface ILevel5TransactionService {
    void publishBatch(Batch batch);
}