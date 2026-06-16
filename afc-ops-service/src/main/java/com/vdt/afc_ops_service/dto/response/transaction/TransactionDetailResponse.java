package com.vdt.afc_ops_service.dto.response.transaction;

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
public class TransactionDetailResponse {
    String id;
    String eventId;
    Long operatorId;
    String operatorCode;
    String operatorName;
    Long routeId;
    String routeCode;
    String routeName;
    Long stationId;
    String stationCode;
    String stationName;
    Long deviceId;
    String deviceCode;
    String deviceType;
    String deviceDirection;
    String mediaType;
    String cardId;
    String cardUid;
    String cardStatus;
    String ticketId;
    String ticketUsageStatus;
    String entitlementId;
    String entitlementStatus;
    String qrId;
    String qrPayloadHash;
    String tapType;
    String journeyRef;
    LocalDateTime occurredAt;
    LocalDateTime receivedAt;
    String decision;
    String reason;
    String syncStatus;
    String ticketProcessingStatus;
    String batchId;
    String rawEventRef;
    Boolean rawEventAvailable;
    Boolean ticketUsageResultAvailable;
    Boolean auditAvailable;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
