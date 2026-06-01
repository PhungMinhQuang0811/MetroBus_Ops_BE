package com.vdt.authservice.modules.identity.validation;

import com.vdt.authservice.modules.identity.validation.validator.PhoneNumberValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface PhoneNumberConstraint {
    String message() default "INVALID_PHONE_NUMBER";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
