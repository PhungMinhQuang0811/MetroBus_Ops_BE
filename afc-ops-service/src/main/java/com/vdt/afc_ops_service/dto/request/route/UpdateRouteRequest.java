package com.vdt.afc_ops_service.dto.request.route;

import com.vdt.afc_ops_service.validation.AllowedTransportType;
import com.vdt.afc_ops_service.validation.RequiredField;
import jakarta.validation.constraints.Size;
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
public class UpdateRouteRequest {
    @RequiredField(fieldName = "routeName")
    @Size(max = 255, message = "INVALID_ROUTE_NAME_LENGTH")
    String routeName;

    @RequiredField(fieldName = "transportType")
    @AllowedTransportType
    String transportType;
}
