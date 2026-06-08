package com.vdt.auth_ops_service.dto.response.account;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportAccountItemResponse {
    Integer row;
    String id;
    String username;
    Set<String> roles;
    Boolean isActive;
    String passwordStatus;
    String temporaryPassword;
}
