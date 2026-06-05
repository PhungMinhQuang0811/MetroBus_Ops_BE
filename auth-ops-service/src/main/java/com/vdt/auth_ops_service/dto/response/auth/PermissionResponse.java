package com.vdt.auth_ops_service.dto.response.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PermissionResponse {
    int id;
    String name;
    String description;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
