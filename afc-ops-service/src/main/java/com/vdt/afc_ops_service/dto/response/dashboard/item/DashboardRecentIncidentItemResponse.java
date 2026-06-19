package com.vdt.afc_ops_service.dto.response.dashboard.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class DashboardRecentIncidentItemResponse {
    String incidentId;
    LocalDateTime occurredAt;
    Long stationId;
    String stationCode;
    Long deviceId;
    String deviceCode;
    String severity;
    String incidentType;
    boolean resolved;
}
