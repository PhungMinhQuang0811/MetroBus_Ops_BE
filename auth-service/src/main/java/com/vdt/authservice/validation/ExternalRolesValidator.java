package com.vdt.authservice.validation;

import com.vdt.authservice.constant.PredefinedRole;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class ExternalRolesValidator implements ConstraintValidator<ExternalRolesConstraint, Set<String>> {
    private static final Set<String> ALLOWED_ROLES = Set.of(
            PredefinedRole.PASSENGER
    );

    @Override
    public boolean isValid(Set<String> roles, ConstraintValidatorContext context) {
        if (roles == null || roles.isEmpty()) {
            return true; // Cho phép trống ở đây, việc bắt trống sẽ do @RequiredField đảm nhiệm
        }
        
        // Kiểm tra xem tất cả các role được chọn có nằm trong danh sách ALLOWED_ROLES không
        return ALLOWED_ROLES.containsAll(roles);
    }
}
