package com.vdt.auth_ops_service.dto.response.account;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportAccountPreviewItem {
    Integer row;
    String username;
    String operatorCode;
    String roleName;
    Boolean valid;
    List<ImportAccountRowError> errors;
}
