package com.vdt.afc_ops_service.validation.validator;

import com.vdt.afc_ops_service.constant.PredefinedDeviceDirection;
import com.vdt.afc_ops_service.validation.AllowedDeviceDirection;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

public class AllowedDeviceDirectionValidator implements ConstraintValidator<AllowedDeviceDirection, String> {

    private static final Set<String> ALLOWED_DIRECTIONS = Set.of(
            PredefinedDeviceDirection.ENTRY,
            PredefinedDeviceDirection.EXIT,
            PredefinedDeviceDirection.BOTH
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return ALLOWED_DIRECTIONS.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}
