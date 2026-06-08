package com.vdt.auth_ops_service.util;

import com.vdt.auth_ops_service.common.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilTest {
    @Test
    void generateTemporaryPassword_MatchesPasswordRule() {
        String password = PasswordUtil.generateTemporaryPassword();

        assertEquals(9, password.length());
        assertTrue(password.chars().anyMatch(Character::isUpperCase));
        assertTrue(password.chars().anyMatch(Character::isLowerCase));
        assertTrue(password.chars().anyMatch(Character::isDigit));
    }
}
