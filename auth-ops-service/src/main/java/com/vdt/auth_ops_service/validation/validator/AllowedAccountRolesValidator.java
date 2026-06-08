package com.vdt.auth_ops_service.validation.validator;

import com.vdt.auth_ops_service.constant.PredefinedRole;
import com.vdt.auth_ops_service.validation.AllowedAccountRoles;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

public class AllowedAccountRolesValidator implements ConstraintValidator<AllowedAccountRoles, Set<String>> {
    private static final Set<String> ALLOWED_ROLES = Set.of(
            PredefinedRole.OPERATOR_MANAGER,
            PredefinedRole.STATION_OPERATOR
    );

    @Override
    public boolean isValid(Set<String> value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        return value.stream().allMatch(role -> role != null && ALLOWED_ROLES.contains(role));
    }
}
