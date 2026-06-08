package com.vdt.auth_ops_service.dto.response.account;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportAccountPreviewResponse {
    Integer totalRows;
    Integer validRows;
    Integer invalidRows;
    List<ImportAccountPreviewItem> items;
    List<ImportAccountRowError> errors;
}
