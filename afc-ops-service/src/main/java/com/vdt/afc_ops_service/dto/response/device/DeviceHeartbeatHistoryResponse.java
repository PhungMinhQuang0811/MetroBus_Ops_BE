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
public class DeviceHeartbeatHistoryResponse {
    String id;
    Long deviceId;
    String deviceCode;
    Long stationId;
    String status;
    String firmwareVersion;
    LocalDateTime sentAt;
    LocalDateTime receivedAt;
    Map<String, Object> payload;
}
