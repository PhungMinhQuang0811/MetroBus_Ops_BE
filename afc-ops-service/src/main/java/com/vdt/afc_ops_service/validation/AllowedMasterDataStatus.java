package com.vdt.afc_ops_service.validation;

import com.vdt.afc_ops_service.validation.validator.AllowedMasterDataStatusValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AllowedMasterDataStatusValidator.class)
public @interface AllowedMasterDataStatus {
    String message() default "INVALID_MASTER_DATA_STATUS";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
