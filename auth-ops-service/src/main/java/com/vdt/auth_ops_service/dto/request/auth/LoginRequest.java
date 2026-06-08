package com.vdt.auth_ops_service.dto.request.auth;

import com.vdt.auth_ops_service.validation.RequiredField;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {
    @RequiredField(fieldName = "username")
    String username;

    @RequiredField(fieldName = "Password")
    String password;
}
