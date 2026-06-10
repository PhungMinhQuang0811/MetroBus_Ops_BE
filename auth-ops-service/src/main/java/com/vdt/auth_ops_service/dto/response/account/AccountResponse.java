package com.vdt.auth_ops_service.dto.response.account;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AccountResponse {
    String id;
    String username;
    String operatorCode;
    Set<String> roles;
    Boolean isActive;
    String passwordStatus;
    String temporaryPassword;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
