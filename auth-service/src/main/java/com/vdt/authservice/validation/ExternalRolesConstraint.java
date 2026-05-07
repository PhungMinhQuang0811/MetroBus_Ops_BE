package com.vdt.authservice.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ExternalRolesValidator.class)
@Documented
public @interface ExternalRolesConstraint {
    String message() default "INVALID_ROLE_SELECTION";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
