package com.vdt.afc_ops_service.dto.response.controlpackage;

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
public class ControlPackageResponse {
    Long id;
    Long version;
    String packageType;
    String sourceType;
    String status;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
