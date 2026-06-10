package com.vdt.auth_ops_service.security.util;

import com.nimbusds.jwt.SignedJWT;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.entity.Permission;
import com.vdt.auth_ops_service.common.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @InjectMocks
    private JwtUtil jwtUtil;

    private Account mockAccount;
    // Secret key phải đủ 64 bytes cho HS512
    private final String testSecret = "z89u34ht983hg983hg983hg983hg983hg983hg983hg983hg983hg983hg983hg983hg98";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtUtil, "accessTokenSecretKey", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenSecretKey", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtUtil, "refreshTokenExpiration", 86400000L);

        Role role = Role.builder().name("OPERATOR_MANAGER").build();
        mockAccount = Account.builder()
                .id("user-123")
                .username("testuser")
                .operatorCode("HCMC-METRO")
                .roles(Set.of(role))
                .build();
    }

    @Test
    void generateToken_And_Verify_Success() throws Exception {
        // Generate
        String token = jwtUtil.generateToken(mockAccount);
        assertNotNull(token);

        // Verify
        SignedJWT signedJWT = jwtUtil.verifyAccessToken(token);
        assertEquals("user-123", signedJWT.getJWTClaimsSet().getSubject());
        assertEquals("testuser", signedJWT.getJWTClaimsSet().getClaim("username"));
        assertEquals("HCMC-METRO", signedJWT.getJWTClaimsSet().getClaim("operatorCode"));
    }

    @Test
    void generateRefreshToken_And_Verify_Success() throws Exception {
        // Generate
        String token = jwtUtil.generateRefreshToken(mockAccount);
        assertNotNull(token);

        // Verify
        SignedJWT signedJWT = jwtUtil.verifyRefreshToken(token);
        assertEquals("user-123", signedJWT.getJWTClaimsSet().getSubject());
    }

    @Test
    void getExpirationAt_Success() {
        String token = jwtUtil.generateToken(mockAccount);
        Instant expiration = jwtUtil.getExpirationAtFromAccessToken(token);
        assertNotNull(expiration);
        assertTrue(expiration.isAfter(Instant.now()));
    }

    @Test
    void verifyToken_InvalidToken_ThrowsException() {
        assertThrows(RuntimeException.class, () -> jwtUtil.verifyAccessToken("invalid.token.here"));
    }

    @Test
    void generateToken_NoRoles_Success() throws Exception {
        mockAccount.setRoles(null);
        String token = jwtUtil.generateToken(mockAccount);
        SignedJWT signedJWT = jwtUtil.verifyAccessToken(token);
        assertEquals("", signedJWT.getJWTClaimsSet().getClaim("scope"));
    }

    @Test
    void generateToken_RolesNoPermissions_Success() throws Exception {
        Role role = Role.builder().name("USER").permissions(null).build();
        mockAccount.setRoles(Set.of(role));
        String token = jwtUtil.generateToken(mockAccount);
        SignedJWT signedJWT = jwtUtil.verifyAccessToken(token);
        assertEquals("", signedJWT.getJWTClaimsSet().getClaim("scope"));
    }

    @Test
    void generateToken_WithRolesAndPermissions_Success() throws Exception {
        Permission p1 = Permission.builder().name("READ").build();
        Role role = Role.builder().name("USER").permissions(Set.of(p1)).build();
        mockAccount.setRoles(Set.of(role));
        
        String token = jwtUtil.generateToken(mockAccount);
        SignedJWT signedJWT = jwtUtil.verifyAccessToken(token);
        String scope = (String) signedJWT.getJWTClaimsSet().getClaim("scope");
        assertFalse(scope.contains("USER"));
        assertTrue(scope.contains("READ"));
    }

    @Test
    void verifyAccessToken_WrongSignature_ThrowsException() {
        // Tạo token bằng secret này nhưng verify bằng secret khác (giả lập bằng cách đổi secret giữa chừng)
        String token = jwtUtil.generateToken(mockAccount);
        
        ReflectionTestUtils.setField(jwtUtil, "accessTokenSecretKey", "differentSecretKeyDifferentSecretKeyDifferentSecretKeyDifferentSecretKey");
        assertThrows(AppException.class, () -> jwtUtil.verifyAccessToken(token));
    }

    @Test
    void getExpiration_InvalidToken_ThrowsException() {
        assertThrows(AppException.class, () -> jwtUtil.getExpirationAtFromAccessToken("invalid-token"));
    }

    @Test
    void verifyAccessToken_MalformedToken_ThrowsException() {
        assertThrows(AppException.class, () -> jwtUtil.verifyAccessToken("not-a-jwt"));
    }

    @Test
    void verifyAccessToken_ExpiredToken_ThrowsException() {
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", -1000L);
        String token = jwtUtil.generateToken(mockAccount);
        ReflectionTestUtils.setField(jwtUtil, "accessTokenExpiration", 3600000L);
        assertThrows(AppException.class, () -> jwtUtil.verifyAccessToken(token));
    }

    @Test
    void getExpirationAtFromRefreshToken_Success() {
        String token = jwtUtil.generateRefreshToken(mockAccount);
        assertNotNull(jwtUtil.getExpirationAtFromRefreshToken(token));
    }
}
