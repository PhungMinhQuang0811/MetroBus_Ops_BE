package com.vdt.authservice.dto.request.user;

import com.vdt.authservice.validation.RequiredField;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResendVerificationRequest {
    @RequiredField(fieldName = "Email")
    @Email(message = "INVALID_EMAIL")
    String email;
}
