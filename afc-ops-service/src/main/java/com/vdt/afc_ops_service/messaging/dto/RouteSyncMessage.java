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
public class RouteSyncMessage {
    private String routeCode;
    private String routeName;
    private String transportType;
    private String operatorCode;
    private String status;
}
