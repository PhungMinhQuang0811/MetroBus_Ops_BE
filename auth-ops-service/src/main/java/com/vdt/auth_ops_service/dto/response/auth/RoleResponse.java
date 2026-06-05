package com.vdt.auth_ops_service.dto.response.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RoleResponse {
    int id;
    String name;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    Set<PermissionResponse> permissions;
}
