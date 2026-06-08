package com.vdt.auth_ops_service.dto.request.account;

import com.vdt.auth_ops_service.validation.AllowedAccountRoles;
import com.vdt.auth_ops_service.validation.RequiredField;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAccountRequest {
    @RequiredField(fieldName = "username")
    String username;

    @NotEmpty(message = "INVALID_ROLE_SELECTION")
    @AllowedAccountRoles
    Set<String> roleNames;
}
