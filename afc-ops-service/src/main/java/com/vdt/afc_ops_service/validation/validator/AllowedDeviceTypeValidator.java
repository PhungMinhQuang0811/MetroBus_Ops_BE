package com.vdt.afc_ops_service.validation.validator;

import com.vdt.afc_ops_service.constant.PredefinedDeviceType;
import com.vdt.afc_ops_service.validation.AllowedDeviceType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

public class AllowedDeviceTypeValidator implements ConstraintValidator<AllowedDeviceType, String> {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            PredefinedDeviceType.QR_SCANNER_SIMULATOR
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return ALLOWED_TYPES.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}
