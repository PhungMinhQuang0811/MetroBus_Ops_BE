package com.vdt.afc_ops_service.dto.response.route;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportRouteItemResponse {
    Integer row;
    Long id;
    Long operatorId;
    String routeCode;
    String routeName;
    String transportType;
    String status;
}
