package com.vdt.afc_ops_service.dto.response.route;

import com.vdt.afc_ops_service.dto.response.station.StationResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RouteDetailResponse {
    Long id;
    Long operatorId;
    String routeCode;
    String routeName;
    String transportType;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Integer stationCount;
    List<StationResponse> stations;
}
