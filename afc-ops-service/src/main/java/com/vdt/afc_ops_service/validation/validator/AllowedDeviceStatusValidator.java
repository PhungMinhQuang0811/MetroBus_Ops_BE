package com.vdt.afc_ops_service.validation.validator;

import com.vdt.afc_ops_service.constant.PredefinedDeviceStatus;
import com.vdt.afc_ops_service.validation.AllowedDeviceStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

public class AllowedDeviceStatusValidator implements ConstraintValidator<AllowedDeviceStatus, String> {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            PredefinedDeviceStatus.ACTIVE,
            PredefinedDeviceStatus.OFFLINE,
            PredefinedDeviceStatus.MAINTENANCE,
            PredefinedDeviceStatus.DISABLED
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return ALLOWED_STATUSES.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}
