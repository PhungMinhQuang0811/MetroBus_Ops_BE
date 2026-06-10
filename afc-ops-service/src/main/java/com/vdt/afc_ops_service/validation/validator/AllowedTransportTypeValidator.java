package com.vdt.afc_ops_service.validation.validator;

import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.validation.AllowedTransportType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

public class AllowedTransportTypeValidator implements ConstraintValidator<AllowedTransportType, String> {

    private static final Set<String> ALLOWED_TRANSPORT_TYPES = Set.of(
            PredefinedTransportType.METRO,
            PredefinedTransportType.BUS
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return ALLOWED_TRANSPORT_TYPES.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}
