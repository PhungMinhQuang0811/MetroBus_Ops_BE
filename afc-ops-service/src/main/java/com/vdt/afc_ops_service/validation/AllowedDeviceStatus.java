package com.vdt.afc_ops_service.validation;

import com.vdt.afc_ops_service.validation.validator.AllowedDeviceStatusValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowedDeviceStatusValidator.class)
public @interface AllowedDeviceStatus {
    String message() default "INVALID_DEVICE_STATUS";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
