package com.vdt.afc_ops_service.dto.response.device;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DeviceDetailResponse {
    Long id;
    String deviceCode;
    String deviceType;
    String direction;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    Long stationId;
    String stationCode;
    String stationName;
    Long routeId;
    String routeCode;
    String routeName;

    LocalDateTime lastSeenAt;
    String firmwareVersion;
    DeviceLatestIncidentResponse latestIncident;
}
