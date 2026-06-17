package com.vdt.afc_ops_service.dto.response.device;

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
public class DeviceIncidentDetailResponse {
    String id;
    Long deviceId;
    String deviceCode;
    String deviceType;
    String deviceStatus;
    Long stationId;
    String stationCode;
    String stationName;
    Long routeId;
    String routeCode;
    String routeName;
    String incidentType;
    String severity;
    String message;
    LocalDateTime occurredAt;
    LocalDateTime receivedAt;
    LocalDateTime resolvedAt;
    Map<String, Object> payload;
}
