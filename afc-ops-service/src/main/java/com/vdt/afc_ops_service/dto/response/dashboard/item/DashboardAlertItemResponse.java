package com.vdt.afc_ops_service.dto.response.dashboard.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class DashboardAlertItemResponse {
    String type;
    String severity;
    String message;
    String resourceType;
    String resourceId;
}
