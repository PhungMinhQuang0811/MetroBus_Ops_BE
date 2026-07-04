package com.vdt.afc_ops_service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "integration_exchange_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationExchangeLog {
    @Id
    private String id;

    @Indexed
    @Field("timestamp")
    private LocalDateTime timestamp;

    @Indexed
    @Field("system_name")
    private String systemName;

    @Indexed
    @Field("direction")
    private String direction; // e.g. "OUTBOUND", "INBOUND"

    @Field("endpoint")
    private String endpoint;

    @Indexed
    @Field("status")
    private String status; // "SUCCESS", "FAILED"

    @Field("request_payload")
    private Object requestPayload; // Store JSON as Object or String

    @Field("response_payload")
    private Object responsePayload; // Store JSON as Object or String
    
    @Field("error_message")
    private String errorMessage;
}
