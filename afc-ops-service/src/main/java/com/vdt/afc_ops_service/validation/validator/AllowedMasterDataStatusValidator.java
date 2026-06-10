package com.vdt.afc_ops_service.validation.validator;

import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.validation.AllowedMasterDataStatus;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;
import java.util.Set;

public class AllowedMasterDataStatusValidator implements ConstraintValidator<AllowedMasterDataStatus, String> {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            PredefinedMasterDataStatus.ACTIVE,
            PredefinedMasterDataStatus.DISABLED
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return ALLOWED_STATUSES.contains(value.trim().toUpperCase(Locale.ROOT));
    }
}
