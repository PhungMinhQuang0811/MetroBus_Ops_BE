package com.vdt.auth_ops_service.validation.validator;

import com.vdt.auth_ops_service.validation.RequiredField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Collection;

public class RequiredFieldValidator implements ConstraintValidator<RequiredField, Object> {
    @Override
    public void initialize(RequiredField constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) return false;
        
        if (value instanceof String s) {
            return !s.trim().isEmpty();
        }
        
        if (value instanceof Collection<?> c) {
            return !c.isEmpty();
        }
        
        return true;
    }
}
