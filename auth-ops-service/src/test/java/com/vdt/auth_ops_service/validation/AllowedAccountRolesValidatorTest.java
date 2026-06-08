package com.vdt.auth_ops_service.validation;

import com.vdt.auth_ops_service.constant.PredefinedRole;
import com.vdt.auth_ops_service.validation.validator.AllowedAccountRolesValidator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllowedAccountRolesValidatorTest {
    private final AllowedAccountRolesValidator validator = new AllowedAccountRolesValidator();

    @Test
    void isValid_NullOrEmpty_ReturnsTrue() {
        assertTrue(validator.isValid(null, null));
        assertTrue(validator.isValid(Set.of(), null));
    }

    @Test
    void isValid_AllowedRoles_ReturnsTrue() {
        assertTrue(validator.isValid(Set.of(
                PredefinedRole.OPERATOR_MANAGER,
                PredefinedRole.STATION_OPERATOR
        ), null));
    }

    @Test
    void isValid_DisallowedOrNullRole_ReturnsFalse() {
        assertFalse(validator.isValid(Set.of(PredefinedRole.OPERATOR_ADMIN), null));
        Set<String> roles = new HashSet<>();
        roles.add(PredefinedRole.STATION_OPERATOR);
        roles.add(null);
        assertFalse(validator.isValid(roles, null));
    }
}
