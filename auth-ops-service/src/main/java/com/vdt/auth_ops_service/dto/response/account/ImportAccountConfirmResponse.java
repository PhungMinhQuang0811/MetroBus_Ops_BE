package com.vdt.auth_ops_service.dto.response.account;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportAccountConfirmResponse {
    Integer imported;
    List<ImportAccountItemResponse> items;
}
