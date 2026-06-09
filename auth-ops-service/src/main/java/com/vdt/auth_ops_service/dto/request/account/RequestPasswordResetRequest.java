package com.vdt.auth_ops_service.dto.request.account;

import com.vdt.auth_ops_service.validation.RequiredField;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RequestPasswordResetRequest {
    @RequiredField(fieldName = "username")
    String username;
}
