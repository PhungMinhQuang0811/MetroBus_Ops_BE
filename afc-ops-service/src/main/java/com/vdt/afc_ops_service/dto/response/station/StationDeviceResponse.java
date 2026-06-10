package com.vdt.afc_ops_service.dto.response.station;

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
public class StationDeviceResponse {
    Long id;
    String deviceCode;
    String deviceType;
    String direction;
    String status;
    String firmwareVersion;
    LocalDateTime lastSeenAt;
}
