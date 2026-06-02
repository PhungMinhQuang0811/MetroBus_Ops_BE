package com.vdt.authservice.modules.identity.dto.request.auth;

import com.vdt.authservice.modules.identity.validation.PhoneNumberConstraint;
import com.vdt.authservice.modules.identity.validation.RequiredField;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhoneCheckRequest {
    @RequiredField(fieldName = "Phone Number")
    @PhoneNumberConstraint
    String phoneNumber;
}
