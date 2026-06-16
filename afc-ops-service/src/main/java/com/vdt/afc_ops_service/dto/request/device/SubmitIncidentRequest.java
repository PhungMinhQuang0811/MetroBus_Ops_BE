package com.vdt.afc_ops_service.dto.request.device;

import com.vdt.afc_ops_service.validation.RequiredField;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitIncidentRequest {

    @RequiredField(fieldName = "incidentType")
    @Size(max = 100, message = "INVALID_SEARCH_KEYWORD")
    String incidentType;

    @RequiredField(fieldName = "severity")
    @Size(max = 30, message = "INVALID_MASTER_DATA_STATUS")
    String severity;

    @RequiredField(fieldName = "occurredAt")
    LocalDateTime occurredAt;

    @Size(max = 255, message = "INVALID_ROUTE_NAME_LENGTH")
    String message;

    Map<String, Object> payload;
}
