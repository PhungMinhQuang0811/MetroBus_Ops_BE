package com.vdt.auth_ops_service.validation;

import com.vdt.auth_ops_service.validation.validator.AllowedAccountRolesValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowedAccountRolesValidator.class)
public @interface AllowedAccountRoles {
    String message() default "INVALID_ROLE_SELECTION";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
