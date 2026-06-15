package com.vdt.afc_ops_service.dto.request.transaction;

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
public class SubmitTransactionRequest {

    @RequiredField(fieldName = "deviceCode")
    @Size(max = 100, message = "INVALID_DEVICE_CODE_LENGTH")
    String deviceCode;

    @RequiredField(fieldName = "deviceSecret")
    @Size(max = 255, message = "INVALID_DEVICE_SECRET_LENGTH")
    String deviceSecret;

    @RequiredField(fieldName = "qrPayload")
    @Size(max = 255, message = "INVALID_SEARCH_KEYWORD")
    String qrPayload;
}
