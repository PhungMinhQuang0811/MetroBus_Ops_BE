package com.vdt.afc_ops_service.dto.response.route;

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
public class RouteResponse {
    Long id;
    Long operatorId;
    String routeCode;
    String routeName;
    String transportType;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
