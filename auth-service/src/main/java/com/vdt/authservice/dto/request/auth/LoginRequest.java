package com.vdt.authservice.dto.request.auth;

import com.vdt.authservice.validation.RequiredField;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {
    @RequiredField(fieldName = "Email")
    String email;

    @RequiredField(fieldName = "Password")
    String password;
}
