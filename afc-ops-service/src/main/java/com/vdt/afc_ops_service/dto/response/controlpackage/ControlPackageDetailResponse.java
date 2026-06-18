package com.vdt.afc_ops_service.dto.response.controlpackage;

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
public class ControlPackageDetailResponse {
    Long id;
    Long version;
    String packageType;
    String sourceType;
    String status;
    Map<String, Object> payload;
    String createdByAccountId;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDateTime publishedAt;
}
