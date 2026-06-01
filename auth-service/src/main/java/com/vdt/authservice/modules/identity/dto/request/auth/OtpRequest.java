package com.vdt.authservice.modules.identity.dto.request.auth;

import com.vdt.authservice.modules.identity.validation.RequiredField;
import com.vdt.authservice.modules.identity.validation.PhoneNumberConstraint;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpRequest {
    @RequiredField(message = "Phone Number")
    @PhoneNumberConstraint
    String phoneNumber;
}
