package com.vdt.authservice.modules.identity.dto.response.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RegistrationOtpResponse {
    String registrationToken;
    String nextStep;
}
