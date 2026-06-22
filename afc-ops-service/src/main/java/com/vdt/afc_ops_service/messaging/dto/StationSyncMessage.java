package com.vdt.afc_ops_service.messaging.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StationSyncMessage {
    private String stationCode;
    private String stationName;
    private Integer stationOrder;
    private String routeCode;
    private String status;
}
