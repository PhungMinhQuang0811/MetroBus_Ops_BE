package com.vdt.afc_ops_service.dto.response.controlpackage;

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
public class ControlPackageSyncResponse {
    Long syncId;
    Long stationId;
    String stationCode;
    String stationName;
    Long packageId;
    String packageType;
    Long version;
    String syncStatus;
    Integer retryCount;
    LocalDateTime lastAttemptAt;
    LocalDateTime appliedAt;
    LocalDateTime updatedAt;
    String errorMessage;
}
