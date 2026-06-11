package com.vdt.afc_ops_service.dto.request.device;

import com.vdt.afc_ops_service.validation.AllowedDeviceDirection;
import com.vdt.afc_ops_service.validation.AllowedDeviceType;
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
public class CreateDeviceRequest {
    @RequiredField(fieldName = "stationId")
    @Min(value = 1, message = "INVALID_STATION_ID")
    Long stationId;

    @RequiredField(fieldName = "deviceType")
    @AllowedDeviceType
    String deviceType;

    @RequiredField(fieldName = "direction")
    @AllowedDeviceDirection
    String direction;

    @Size(max = 100, message = "INVALID_FIRMWARE_VERSION_LENGTH")
    String firmwareVersion;
}
