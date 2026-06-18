package com.vdt.afc_ops_service.dto.response.controlpackage;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PendingControlPackageResponse {
    Long syncId;
    Long packageId;
    Long version;
    String packageType;
    String sourceType;
    Map<String, Object> payload;
}
