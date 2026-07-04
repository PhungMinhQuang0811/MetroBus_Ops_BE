package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.document.IntegrationExchangeLog;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.audit.IntegrationExchangeLogResponse;
import com.vdt.afc_ops_service.repository.mongo.IntegrationExchangeLogRepository;
import com.vdt.afc_ops_service.service.IIntegrationExchangeLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IntegrationExchangeLogService implements IIntegrationExchangeLogService {

    private final IntegrationExchangeLogRepository repository;

    @Override
    public void logExchange(String systemName, String direction, String endpoint, String status, Object requestPayload, Object responsePayload, String errorMessage) {
        try {
            IntegrationExchangeLog logEntry = IntegrationExchangeLog.builder()
                    .timestamp(LocalDateTime.now())
                    .systemName(systemName)
                    .direction(direction)
                    .endpoint(endpoint)
                    .status(status)
                    .requestPayload(requestPayload)
                    .responsePayload(responsePayload)
                    .errorMessage(errorMessage)
                    .build();
            
            repository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to save integration exchange log", e);
        }
    }

    @Override
    public PageResponse<IntegrationExchangeLogResponse> getLogs(int page, int size, String systemName, String direction, String status, LocalDateTime start, LocalDateTime end) {
        Pageable pageable = PageRequest.of(page, size);
        Page<IntegrationExchangeLog> logPage;

        if (start == null) {
            start = LocalDateTime.now().minusDays(7);
        }
        if (end == null) {
            end = LocalDateTime.now();
        }

        if (systemName != null && !systemName.isEmpty()) {
            logPage = repository.findBySystemNameAndTimestampBetweenOrderByTimestampDesc(systemName, start, end, pageable);
        } else if (direction != null && !direction.isEmpty()) {
            logPage = repository.findByDirectionAndTimestampBetweenOrderByTimestampDesc(direction, start, end, pageable);
        } else if (status != null && !status.isEmpty()) {
            logPage = repository.findByStatusAndTimestampBetweenOrderByTimestampDesc(status, start, end, pageable);
        } else {
            logPage = repository.findByTimestampBetweenOrderByTimestampDesc(start, end, pageable);
        }

        return PageResponse.<IntegrationExchangeLogResponse>builder()
                .items(logPage.getContent().stream().map(this::mapToResponse).collect(Collectors.toList()))
                .page(logPage.getNumber())
                .size(logPage.getSize())
                .totalElements(logPage.getTotalElements())
                .totalPages(logPage.getTotalPages())
                .build();
    }

    private IntegrationExchangeLogResponse mapToResponse(IntegrationExchangeLog log) {
        return IntegrationExchangeLogResponse.builder()
                .id(log.getId())
                .timestamp(log.getTimestamp())
                .systemName(log.getSystemName())
                .direction(log.getDirection())
                .endpoint(log.getEndpoint())
                .status(log.getStatus())
                .requestPayload(log.getRequestPayload())
                .responsePayload(log.getResponsePayload())
                .errorMessage(log.getErrorMessage())
                .build();
    }
}
