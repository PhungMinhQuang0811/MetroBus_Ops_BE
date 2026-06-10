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
public class StationResponse {
    Long id;
    Long routeId;
    String routeCode;
    String stationCode;
    String stationName;
    Integer stationOrder;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
