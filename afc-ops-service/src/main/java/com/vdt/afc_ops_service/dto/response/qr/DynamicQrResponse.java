package com.vdt.afc_ops_service.dto.response.qr;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DynamicQrResponse {
    String qrId;
    String qrPayload;
    LocalDateTime expiresAt;
    Integer refreshAfterSeconds;
}
