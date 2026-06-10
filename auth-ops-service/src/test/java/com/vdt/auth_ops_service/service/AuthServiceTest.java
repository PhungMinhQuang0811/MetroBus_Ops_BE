package com.vdt.auth_ops_service.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vdt.auth_ops_service.constant.PredefinedPasswordStatus;
import com.vdt.auth_ops_service.dto.request.account.RequestPasswordResetRequest;
import com.vdt.auth_ops_service.dto.request.auth.LoginRequest;
import com.vdt.auth_ops_service.dto.response.account.RequestPasswordResetResponse;
import com.vdt.auth_ops_service.dto.response.auth.AuthResponse;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.service.Impl.AuthService;
import com.vdt.auth_ops_service.mapper.AuthMapper;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.security.service.ITokenManagementService;
import com.vdt.auth_ops_service.security.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private AuthMapper authMapper;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private ITokenManagementService tokenManagementService;
    @Mock private HttpServletResponse response;
    @Mock private HttpServletRequest request;

    @InjectMocks
    private AuthService authService;

    private Account mockAccount;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenCookieName", "access-token");
        ReflectionTestUtils.setField(authService, "refreshTokenCookieName", "refresh-token");
        ReflectionTestUtils.setField(authService, "csrfCookieName", "XSRF-TOKEN");
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 86400000L);
        ReflectionTestUtils.setField(authService, "domain", "localhost");
        ReflectionTestUtils.setField(authService, "contextPath", "/");
        ReflectionTestUtils.setField(authService, "refreshPath", "/auth/refresh");
        ReflectionTestUtils.setField(authService, "logoutPath", "/auth/logout");

        mockAccount = Account.builder()
                .id("acc-123")
                .username("testuser")
                .operatorCode("HCMC-METRO")
                .isActive(true)
                .build();
    }

    @Test
    void login_Success() {
        LoginRequest req = new LoginRequest("testuser", "password");
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(mockAccount));
        when(jwtUtil.generateToken(mockAccount)).thenReturn("at");
        when(jwtUtil.generateRefreshToken(mockAccount)).thenReturn("rt");
        when(authMapper.toAuthResponse(mockAccount)).thenReturn(new AuthResponse());

        AuthResponse res = authService.login(req, request, response);
        assertNotNull(res);
        verify(authenticationManager).authenticate(any());
    }
    @Test
    void login_UserNotFound_ThrowsException() {
        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> authService.login(new LoginRequest("no", "pwd"), request, response));
    }

    @Test
    void logout_Success() throws Exception {
        Cookie atCookie = new Cookie("access-token", "at-value");
        Cookie rtCookie = new Cookie("refresh-token", "rt-value");
        when(request.getCookies()).thenReturn(new Cookie[]{atCookie, rtCookie});
        
        when(jwtUtil.getExpirationAtFromAccessToken("at-value")).thenReturn(Instant.now().plusSeconds(60));
        when(jwtUtil.getExpirationAtFromRefreshToken("rt-value")).thenReturn(Instant.now().plusSeconds(60));
        
        SignedJWT signedJWT = mock(SignedJWT.class);
        when(jwtUtil.verifyRefreshToken("rt-value")).thenReturn(signedJWT);

        authService.logout(request, response);

        verify(tokenManagementService).invalidateAccessToken(eq("at-value"), any());
        verify(tokenManagementService).invalidateRefreshToken(eq("rt-value"), any());
    }

    @Test
    void logout_EmptyCookies_StillWorks() {
        when(request.getCookies()).thenReturn(null);
        authService.logout(request, response);
        verify(response, times(4)).addHeader(eq("Set-Cookie"), anyString());
    }

    @Test
    void refreshToken_Success() throws Exception {
        Cookie rtCookie = new Cookie("refresh-token", "rt-value");
        when(request.getCookies()).thenReturn(new Cookie[]{rtCookie});
        when(tokenManagementService.isRefreshTokenInvalidated("rt-value")).thenReturn(false);
        
        SignedJWT signedJWT = mock(SignedJWT.class);
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(mockAccount.getId()).build();
        when(signedJWT.getJWTClaimsSet()).thenReturn(claimsSet);
        when(jwtUtil.verifyRefreshToken("rt-value")).thenReturn(signedJWT);
        
        when(accountRepository.findById(mockAccount.getId())).thenReturn(Optional.of(mockAccount));
        when(jwtUtil.generateToken(mockAccount)).thenReturn("new-at");
        when(jwtUtil.generateRefreshToken(mockAccount)).thenReturn("new-rt");

        authService.refreshToken(request, response);
        verify(response, atLeastOnce()).addHeader(anyString(), anyString());
    }

    @Test
    void refreshToken_TokenNull_ThrowsException() {
        when(request.getCookies()).thenReturn(null);
        assertThrows(AppException.class, () -> authService.refreshToken(request, response));
    }

    @Test
    void refreshToken_JwtVerifyException_ThrowsException() throws Exception {
        Cookie rtCookie = new Cookie("refresh-token", "invalid-jwt");
        when(request.getCookies()).thenReturn(new Cookie[]{rtCookie});
        when(tokenManagementService.isRefreshTokenInvalidated(anyString())).thenReturn(false);
        when(jwtUtil.verifyRefreshToken(anyString())).thenThrow(new RuntimeException("Invalid JWT"));
        
        assertThrows(AppException.class, () -> authService.refreshToken(request, response));
    }

    @Test
    void refreshToken_AccountNotFound_ThrowsException() throws Exception {
        Cookie rtCookie = new Cookie("refresh-token", "valid-jwt");
        when(request.getCookies()).thenReturn(new Cookie[]{rtCookie});
        when(tokenManagementService.isRefreshTokenInvalidated(anyString())).thenReturn(false);
        
        SignedJWT signedJWT = mock(SignedJWT.class);
        when(signedJWT.getJWTClaimsSet()).thenReturn(new JWTClaimsSet.Builder().subject("no-id").build());
        when(jwtUtil.verifyRefreshToken(anyString())).thenReturn(signedJWT);
        when(accountRepository.findById("no-id")).thenReturn(Optional.empty());
        
        assertThrows(AppException.class, () -> authService.refreshToken(request, response));
    }

    @Test
    void refreshToken_Invalidated_ThrowsException() {
        Cookie rtCookie = new Cookie("refresh-token", "invalidated-rt");
        when(request.getCookies()).thenReturn(new Cookie[]{rtCookie});
        when(tokenManagementService.isRefreshTokenInvalidated("invalidated-rt")).thenReturn(true);
        assertThrows(AppException.class, () -> authService.refreshToken(request, response));
    }

    @Test
    void refreshToken_AccountDisabled_ThrowsException() throws Exception {
        Cookie rtCookie = new Cookie("refresh-token", "rt-value");
        when(request.getCookies()).thenReturn(new Cookie[]{rtCookie});
        when(tokenManagementService.isRefreshTokenInvalidated("rt-value")).thenReturn(false);

        SignedJWT signedJWT = mock(SignedJWT.class);
        when(signedJWT.getJWTClaimsSet()).thenReturn(new JWTClaimsSet.Builder().subject(mockAccount.getId()).build());
        when(jwtUtil.verifyRefreshToken("rt-value")).thenReturn(signedJWT);

        mockAccount.setActive(false);
        when(accountRepository.findById(mockAccount.getId())).thenReturn(Optional.of(mockAccount));

        AppException ex = assertThrows(AppException.class, () -> authService.refreshToken(request, response));
        assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
    }

    @Test
    void requestPasswordReset_Success_SetsPasswordStatusNeedToReset() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(mockAccount));

        RequestPasswordResetResponse response = authService.requestPasswordReset(
                RequestPasswordResetRequest.builder().username("testuser").build()
        );

        assertEquals("testuser", response.getUsername());
        assertEquals(PredefinedPasswordStatus.NEED_TO_RESET, response.getPasswordStatus());
        assertEquals(PredefinedPasswordStatus.NEED_TO_RESET, mockAccount.getPasswordStatus());
        verify(accountRepository).save(mockAccount);
    }

    @Test
    void requestPasswordReset_NotFound_ThrowsException() {
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> authService.requestPasswordReset(RequestPasswordResetRequest.builder().username("testuser").build()));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void requestPasswordReset_DisabledAccount_ThrowsException() {
        mockAccount.setActive(false);
        when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(mockAccount));

        AppException exception = assertThrows(AppException.class,
                () -> authService.requestPasswordReset(RequestPasswordResetRequest.builder().username("testuser").build()));

        assertEquals(ErrorCode.ACCOUNT_DISABLED, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

}
