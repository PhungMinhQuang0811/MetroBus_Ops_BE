package com.vdt.afc_ops_service.service.generator;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class DeviceSecretGenerator {

    static final int SECRET_BYTES = 32;
    final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
