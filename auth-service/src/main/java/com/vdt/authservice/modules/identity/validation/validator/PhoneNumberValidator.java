package com.vdt.authservice.modules.identity.validation.validator;

import com.vdt.authservice.modules.identity.validation.PhoneNumberConstraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class PhoneNumberValidator implements ConstraintValidator<PhoneNumberConstraint, String> {
    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("^(0\\d{9}|\\+84\\d{9})$");

    @Override
    public void initialize(PhoneNumberConstraint constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) return true;
        return PHONE_NUMBER_PATTERN.matcher(value.trim()).matches();
    }
}
