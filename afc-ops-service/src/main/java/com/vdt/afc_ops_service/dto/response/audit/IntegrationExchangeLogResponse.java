package com.vdt.afc_ops_service.dto.response.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationExchangeLogResponse {
    private String id;
    private LocalDateTime timestamp;
    private String systemName;
    private String direction;
    private String endpoint;
    private String status;
    private Object requestPayload;
    private Object responsePayload;
    private String errorMessage;
}
