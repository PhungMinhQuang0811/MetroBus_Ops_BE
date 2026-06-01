package com.vdt.authservice.modules.identity.dto.request.auth;

import com.vdt.authservice.modules.identity.validation.PhoneNumberConstraint;
import com.vdt.authservice.modules.identity.validation.RequiredField;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyOtpRequest {
    @RequiredField(message = "Phone Number")
    @PhoneNumberConstraint
    String phoneNumber;

    @NotBlank(message = "FIELD_REQUIRED")
    @Pattern(regexp = "^\\d{6}$", message = "OTP_INVALID_OR_EXPIRED")
    String otp;
}
