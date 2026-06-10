package com.vdt.afc_ops_service.security.util;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    private static final String SECRET = "01234567890123456789012345678901";

    JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "accessTokenSecretKey", SECRET);
    }

    @Test
    void verifyAccessToken_ValidToken_ReturnsSignedJwt() throws Exception {
        SignedJWT token = buildToken(TokenType.ACCESS_TOKEN, Date.from(Instant.now().plusSeconds(300)));

        SignedJWT verified = jwtUtil.verifyAccessToken(token.serialize());

        assertEquals("account-1", verified.getJWTClaimsSet().getSubject());
        assertEquals("manager", verified.getJWTClaimsSet().getStringClaim("username"));
    }

    @Test
    void verifyAccessToken_ExpiredToken_ThrowsUnauthenticated() throws Exception {
        SignedJWT token = buildToken(TokenType.ACCESS_TOKEN, Date.from(Instant.now().minusSeconds(300)));

        AppException exception = assertThrows(AppException.class, () -> jwtUtil.verifyAccessToken(token.serialize()));

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    void verifyAccessToken_MissingExpiration_ThrowsUnauthenticated() throws Exception {
        SignedJWT token = buildToken(TokenType.ACCESS_TOKEN, null);

        AppException exception = assertThrows(AppException.class, () -> jwtUtil.verifyAccessToken(token.serialize()));

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    void verifyAccessToken_InvalidSignature_ThrowsUnauthenticated() throws Exception {
        SignedJWT token = buildToken(
                TokenType.ACCESS_TOKEN,
                Date.from(Instant.now().plusSeconds(300)),
                "abcdefghijklmnopqrstuvwxyz123456"
        );

        AppException exception = assertThrows(AppException.class, () -> jwtUtil.verifyAccessToken(token.serialize()));

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    void verifyAccessToken_WrongTokenType_ThrowsUnauthenticated() throws Exception {
        SignedJWT token = buildToken("REFRESH_TOKEN", Date.from(Instant.now().plusSeconds(300)));

        AppException exception = assertThrows(AppException.class, () -> jwtUtil.verifyAccessToken(token.serialize()));

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    @Test
    void verifyAccessToken_MalformedToken_ThrowsUnauthenticated() {
        AppException exception = assertThrows(AppException.class, () -> jwtUtil.verifyAccessToken("bad.token.value"));

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
    }

    private SignedJWT buildToken(String tokenType, Date expiryTime) throws Exception {
        return buildToken(tokenType, expiryTime, SECRET);
    }

    private SignedJWT buildToken(String tokenType, Date expiryTime, String signingSecret) throws Exception {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject("account-1")
                .claim("username", "manager")
                .claim("operatorCode", "HCMC-METRO")
                .claim("scope", "MASTER_DATA_READ")
                .claim("tokenType", tokenType)
                .expirationTime(expiryTime)
                .build();

        SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claimsSet);
        signedJWT.sign(new MACSigner(signingSecret));
        return signedJWT;
    }
}
