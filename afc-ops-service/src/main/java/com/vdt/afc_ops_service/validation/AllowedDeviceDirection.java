package com.vdt.afc_ops_service.validation;

import com.vdt.afc_ops_service.validation.validator.AllowedDeviceDirectionValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowedDeviceDirectionValidator.class)
public @interface AllowedDeviceDirection {
    String message() default "INVALID_DEVICE_DIRECTION";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
