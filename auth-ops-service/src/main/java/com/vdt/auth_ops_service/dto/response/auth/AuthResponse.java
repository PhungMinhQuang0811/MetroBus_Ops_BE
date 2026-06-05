package com.vdt.auth_ops_service.dto.response.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthResponse {
    String id;
    String username;
    boolean mustChangePassword;
    Set<String> roles;
    Set<String> permissions;
}
