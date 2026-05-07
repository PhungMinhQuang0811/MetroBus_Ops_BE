package com.vdt.authservice.validation.validator;

import com.vdt.authservice.validation.RequiredField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RequiredFieldValidator implements ConstraintValidator<RequiredField, String> {
    @Override
    public void initialize(RequiredField constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && !value.trim().isEmpty();
    }
}
