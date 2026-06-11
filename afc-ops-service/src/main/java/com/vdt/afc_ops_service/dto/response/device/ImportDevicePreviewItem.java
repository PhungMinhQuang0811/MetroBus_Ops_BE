package com.vdt.afc_ops_service.dto.response.device;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportDevicePreviewItem {
    Integer row;
    Long stationId;
    String stationCode;
    String stationName;
    String deviceType;
    String direction;
    String status;
    String firmwareVersion;
    Boolean valid;
    List<ImportDeviceRowError> errors;
}
