package com.vdt.authservice.modules.identity.dto.response.auth;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PhoneCheckResponse {
    boolean exists;
    String nextStep;
    String phoneNumber;
}
