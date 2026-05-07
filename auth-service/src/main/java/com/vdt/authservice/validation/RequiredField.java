package com.vdt.authservice.validation;

import com.vdt.authservice.validation.validator.RequiredFieldValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = RequiredFieldValidator.class)
public @interface RequiredField {
    String message() default "FIELD_REQUIRED";
    String fieldName() default "";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
