package com.vdt.auth_ops_service.validation.validator;

import com.vdt.auth_ops_service.validation.PasswordConstraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PasswordValidator implements ConstraintValidator<PasswordConstraint, String> {
    private static final String PASSWORD_PATTERN = "^(?=.*[a-zA-Z])(?=.*[0-9]).{9,}$";

    @Override
    public void initialize(PasswordConstraint constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return Pattern.matches(PASSWORD_PATTERN, value);
    }
}
