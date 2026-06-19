package com.vdt.afc_ops_service.dto.response.dashboard.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import static lombok.AccessLevel.PRIVATE;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = PRIVATE)
public class DashboardRouteStationSummaryItemResponse {
    Long routeId;
    String routeCode;
    String routeName;
    Long stationId;
    String stationCode;
    String stationName;
    long total;
    long openGate;
    long deny;
}
