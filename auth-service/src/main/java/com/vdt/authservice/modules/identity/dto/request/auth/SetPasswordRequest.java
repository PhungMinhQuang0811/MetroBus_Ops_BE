package com.vdt.authservice.modules.identity.dto.request.auth;

import com.vdt.authservice.modules.identity.validation.PasswordConstraint;
import com.vdt.authservice.modules.identity.validation.RequiredField;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SetPasswordRequest {
    @RequiredField(fieldName = "Registration Token")
    String registrationToken;

    @RequiredField(fieldName = "Password")
    @PasswordConstraint
    String password;
}
