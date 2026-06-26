package com.vdt.afc_ops_service.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class CryptoHashUtilTest {

    @Test
    void sha256Base64Url_validInput_returnsBase64UrlString() {
        String result = CryptoHashUtil.sha256Base64Url("hello");
        assertNotNull(result);
        assertEquals(43, result.length()); // SHA-256 = 32 bytes, base64url = 43 chars
    }

    @Test
    void sha256Base64Url_sameInput_returnsSameOutput() {
        String a = CryptoHashUtil.sha256Base64Url("hello");
        String b = CryptoHashUtil.sha256Base64Url("hello");
        assertEquals(a, b);
    }

    @Test
    void sha256Base64Url_differentInput_returnsDifferentOutput() {
        String a = CryptoHashUtil.sha256Base64Url("hello");
        String b = CryptoHashUtil.sha256Base64Url("world");
        assertNotEquals(a, b);
    }

    @Test
    void hmacSha256Base64Url_validInput_returnsBase64UrlString() {
        String result = CryptoHashUtil.hmacSha256Base64Url("my-secret", "AFCQR:v1:TICKET-001:exp=1000");
        assertNotNull(result);
        assertNotEquals("", result);
    }

    @Test
    void hmacSha256Base64Url_sameInput_returnsSameOutput() {
        String a = CryptoHashUtil.hmacSha256Base64Url("my-secret", "data");
        String b = CryptoHashUtil.hmacSha256Base64Url("my-secret", "data");
        assertEquals(a, b);
    }

    @Test
    void hmacSha256Base64Url_differentSecret_returnsDifferentOutput() {
        String a = CryptoHashUtil.hmacSha256Base64Url("secret-a", "data");
        String b = CryptoHashUtil.hmacSha256Base64Url("secret-b", "data");
        assertNotEquals(a, b);
    }

    @Test
    void hmacSha256Base64Url_differentData_returnsDifferentOutput() {
        String a = CryptoHashUtil.hmacSha256Base64Url("secret", "data-a");
        String b = CryptoHashUtil.hmacSha256Base64Url("secret", "data-b");
        assertNotEquals(a, b);
    }
}