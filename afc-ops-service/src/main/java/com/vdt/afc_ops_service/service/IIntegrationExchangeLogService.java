package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.audit.IntegrationExchangeLogResponse;

import java.time.LocalDateTime;

public interface IIntegrationExchangeLogService {
    void logExchange(String systemName, String direction, String endpoint, String status, Object requestPayload, Object responsePayload, String errorMessage);
    
    PageResponse<IntegrationExchangeLogResponse> getLogs(int page, int size, String systemName, String direction, String status, LocalDateTime start, LocalDateTime end);
}
