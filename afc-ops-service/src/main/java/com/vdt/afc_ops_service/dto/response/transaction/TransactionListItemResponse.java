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
public class TransactionListItemResponse {
    String id;
    String eventId;
    Long routeId;
    String routeCode;
    String routeName;
    Long stationId;
    String stationCode;
    String stationName;
    Long deviceId;
    String deviceCode;
    String mediaType;
    String cardId;
    String ticketId;
    String entitlementId;
    String qrId;
    String tapType;
    LocalDateTime occurredAt;
    String decision;
    String reason;
    String syncStatus;
    String ticketProcessingStatus;
    String batchId;
}
