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
public class ControlPackageSyncDetailResponse {
    Long syncId;
    Long stationId;
    String stationCode;
    String stationName;
    Long routeId;
    String routeName;
    Long packageId;
    Long version;
    String packageType;
    String sourceType;
    String packageStatus;
    String syncStatus;
    Integer retryCount;
    LocalDateTime lastAttemptAt;
    LocalDateTime appliedAt;
    String errorMessage;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
