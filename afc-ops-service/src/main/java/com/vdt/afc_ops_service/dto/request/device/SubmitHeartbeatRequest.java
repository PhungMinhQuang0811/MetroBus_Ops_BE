package com.vdt.afc_ops_service.dto.request.device;

import com.vdt.afc_ops_service.validation.RequiredField;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SubmitHeartbeatRequest {

    @RequiredField(fieldName = "sentAt")
    LocalDateTime sentAt;

    @RequiredField(fieldName = "status")
    @Size(max = 30, message = "INVALID_DEVICE_STATUS")
    String status;

    @RequiredField(fieldName = "firmwareVersion")
    @Size(max = 100, message = "INVALID_FIRMWARE_VERSION_LENGTH")
    String firmwareVersion;

    Map<String, Object> metrics;
}
