package com.vdt.afc_ops_service.dto.request.station;

import com.vdt.afc_ops_service.validation.RequiredField;
import jakarta.validation.constraints.Min;
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
public class CreateStationRequest {
    @RequiredField(fieldName = "routeId")
    @Min(value = 1, message = "INVALID_ROUTE_ID")
    Long routeId;

    @RequiredField(fieldName = "stationName")
    @Size(max = 255, message = "INVALID_STATION_NAME_LENGTH")
    String stationName;

    @RequiredField(fieldName = "stationOrder")
    @Min(value = 1, message = "INVALID_STATION_ORDER")
    Integer stationOrder;
}
