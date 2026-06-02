package com.vdt.authservice.service;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.vdt.authservice.modules.identity.dto.request.auth.LoginRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.PhoneCheckRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.ResetPasswordRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.SetPasswordRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.VerifyOtpRequest;
import com.vdt.authservice.modules.identity.dto.response.auth.AuthResponse;
import com.vdt.authservice.modules.identity.dto.response.auth.PhoneCheckResponse;
import com.vdt.authservice.modules.identity.dto.response.auth.RegistrationOtpResponse;
import com.vdt.authservice.modules.identity.entity.Account;
import com.vdt.authservice.modules.identity.entity.Role;
import com.vdt.authservice.common.exception.AppException;
import com.vdt.authservice.common.exception.ErrorCode;
import com.vdt.authservice.common.notification.email.IEmailService;
import com.vdt.authservice.modules.identity.service.Impl.AuthService;
import com.vdt.authservice.modules.identity.service.IOtpService;
import com.vdt.authservice.modules.identity.mapper.AuthMapper;
import com.vdt.authservice.modules.identity.repository.AccountRepository;
import com.vdt.authservice.modules.identity.repository.RoleRepository;
import com.vdt.authservice.modules.identity.security.service.IAccountTokenService;
import com.vdt.authservice.modules.identity.security.service.ITokenManagementService;
import com.vdt.authservice.modules.identity.security.util.JwtUtil;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AuthMapper authMapper;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;
    @Mock private IAccountTokenService accountTokenService;
    @Mock private ITokenManagementService tokenManagementService;
    @Mock private IEmailService emailService;
    @Mock private IOtpService otpService;
    @Mock private PasswordEncoder passwordEncoder;
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
                .email("test@example.com")
                .username("testuser")
                .isEmailVerified(true)
                .isActive(true)
                .build();
    }

    @Test
    void login_Success() {
        LoginRequest req = new LoginRequest("testuser", "password");
        when(accountRepository.findByIdentifier("testuser")).thenReturn(Optional.of(mockAccount));
        when(jwtUtil.generateToken(mockAccount)).thenReturn("at");
        when(jwtUtil.generateRefreshToken(mockAccount)).thenReturn("rt");
        when(authMapper.toAuthResponse(mockAccount)).thenReturn(new AuthResponse());

        AuthResponse res = authService.login(req, request, response);
        assertNotNull(res);
        verify(authenticationManager).authenticate(any());
    }
    @Test
    void login_UserNotFound_ThrowsException() {
        when(accountRepository.findByIdentifier(anyString())).thenReturn(Optional.empty());
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
    void forgotPassword_Success() {
        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockAccount));
        when(accountTokenService.generateResetPasswordToken(mockAccount.getId())).thenReturn("reset-token");

        authService.forgotPassword("test@example.com");

        verify(emailService).sendResetPasswordEmail(eq("test@example.com"), eq("reset-token"));
    }

    @Test
    void forgotPassword_UserNotFound_ThrowsException() {
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> authService.forgotPassword("no@example.com"));
    }

    @Test
    void resetPassword_Success() {
        ResetPasswordRequest req = ResetPasswordRequest.builder()
                .token("reset-token")
                .newPassword("new-password")
                .build();
        when(accountTokenService.getAccountIdByResetPasswordToken("reset-token")).thenReturn(mockAccount.getId());
        when(accountRepository.findById(mockAccount.getId())).thenReturn(Optional.of(mockAccount));

        authService.resetPassword(req);

        verify(passwordEncoder).encode("new-password");
        verify(accountRepository).save(mockAccount);
        verify(accountTokenService).deleteResetPasswordToken("reset-token");
    }

    @Test
    void resetPassword_TokenInvalid_ThrowsException() {
        when(accountTokenService.getAccountIdByResetPasswordToken(anyString())).thenReturn(null);
        assertThrows(AppException.class, () -> authService.resetPassword(ResetPasswordRequest.builder().token("invalid").build()));
    }

    @Test
    void forgotPassword_AccountDisabled_ThrowsException() {
        mockAccount.setActive(false);
        when(accountRepository.findByEmail(anyString())).thenReturn(Optional.of(mockAccount));
        assertThrows(AppException.class, () -> authService.forgotPassword("test@example.com"));
    }

    @Test
    void resetPassword_AccountNotFound_ThrowsException() {
        when(accountTokenService.getAccountIdByResetPasswordToken(anyString())).thenReturn("acc-id");
        when(accountRepository.findById("acc-id")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> authService.resetPassword(ResetPasswordRequest.builder().token("token").build()));
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
    void resetPassword_AccountDisabled_ThrowsException() {
        mockAccount.setActive(false);
        when(accountTokenService.getAccountIdByResetPasswordToken(anyString())).thenReturn("acc-id");
        when(accountRepository.findById("acc-id")).thenReturn(Optional.of(mockAccount));
        AppException ex = assertThrows(AppException.class, () -> authService.resetPassword(ResetPasswordRequest.builder().token("token").build()));
        assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
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
    void checkPhone_ExistingAccount_ReturnsPasswordLoginAndDoesNotSendOtp() {
        mockAccount.setPhoneNumber("0900000001");
        when(accountRepository.findByPhoneNumber("0900000001")).thenReturn(Optional.of(mockAccount));

        PhoneCheckResponse result = authService.checkPhone(new PhoneCheckRequest("+84900000001"));

        assertTrue(result.isExists());
        assertEquals("PASSWORD_LOGIN", result.getNextStep());
        assertEquals("0900000001", result.getPhoneNumber());
        verify(otpService, never()).requestOtp(anyString());
    }

    @Test
    void checkPhone_NewAccount_SendsOtpAndReturnsRegisterOtp() {
        when(accountRepository.findByPhoneNumber("0900000002")).thenReturn(Optional.empty());

        PhoneCheckResponse result = authService.checkPhone(new PhoneCheckRequest("0900000002"));

        assertFalse(result.isExists());
        assertEquals("REGISTER_OTP", result.getNextStep());
        assertEquals("0900000002", result.getPhoneNumber());
        verify(otpService).requestOtp("0900000002");
    }

    @Test
    void checkPhone_DisabledExistingAccount_ThrowsException() {
        mockAccount.setActive(false);
        when(accountRepository.findByPhoneNumber("0900000001")).thenReturn(Optional.of(mockAccount));

        AppException ex = assertThrows(AppException.class, () -> authService.checkPhone(new PhoneCheckRequest("0900000001")));

        assertEquals(ErrorCode.ACCOUNT_DISABLED, ex.getErrorCode());
        verify(otpService, never()).requestOtp(anyString());
    }

    @Test
    void verifyRegistrationOtp_NewPhone_ReturnsRegistrationToken() {
        VerifyOtpRequest req = VerifyOtpRequest.builder()
                .phoneNumber("0900000002")
                .otp("123456")
                .build();

        when(accountRepository.existsByPhoneNumber("0900000002")).thenReturn(false);
        when(accountTokenService.generateRegistrationToken("0900000002")).thenReturn("registration-token");

        RegistrationOtpResponse result = authService.verifyRegistrationOtp(req);

        assertEquals("registration-token", result.getRegistrationToken());
        assertEquals("SET_PASSWORD", result.getNextStep());
        verify(otpService).verifyOtp("0900000002", "123456");
    }

    @Test
    void verifyRegistrationOtp_ExistingPhone_ThrowsException() {
        VerifyOtpRequest req = VerifyOtpRequest.builder()
                .phoneNumber("0900000001")
                .otp("123456")
                .build();

        when(accountRepository.existsByPhoneNumber("0900000001")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authService.verifyRegistrationOtp(req));

        assertEquals(ErrorCode.USER_EXISTED, ex.getErrorCode());
        verify(otpService, never()).verifyOtp(anyString(), anyString());
    }

    @Test
    void completeRegistration_ValidToken_CreatesPassengerAccountAndTokensWithoutWallet() {
        SetPasswordRequest req = SetPasswordRequest.builder()
                .registrationToken("registration-token")
                .password("P@ssword123")
                .build();
        Role passengerRole = Role.builder().name("PASSENGER").build();
        Account savedAccount = Account.builder()
                .id("passenger-id")
                .phoneNumber("0900000002")
                .isActive(true)
                .isPhoneVerified(true)
                .roles(Set.of(passengerRole))
                .build();
        AuthResponse authResponse = new AuthResponse();

        when(accountTokenService.getPhoneNumberByRegistrationToken("registration-token")).thenReturn("0900000002");
        when(accountRepository.existsByPhoneNumber("0900000002")).thenReturn(false);
        when(roleRepository.findByName("PASSENGER")).thenReturn(Optional.of(passengerRole));
        when(passwordEncoder.encode("P@ssword123")).thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        when(jwtUtil.generateToken(savedAccount)).thenReturn("at");
        when(jwtUtil.generateRefreshToken(savedAccount)).thenReturn("rt");
        when(authMapper.toAuthResponse(savedAccount)).thenReturn(authResponse);

        AuthResponse result = authService.completeRegistration(req, response);

        assertSame(authResponse, result);
        verify(accountTokenService).deleteRegistrationToken("registration-token");
        verify(response, times(3)).addHeader(eq("Set-Cookie"), anyString());
        verify(accountRepository).save(argThat(account ->
                "0900000002".equals(account.getPhoneNumber())
                        && "encoded-password".equals(account.getPassword())
                        && account.isActive()
                        && account.isPhoneVerified()
        ));
    }

    @Test
    void completeRegistration_InvalidToken_ThrowsException() {
        when(accountTokenService.getPhoneNumberByRegistrationToken("invalid")).thenReturn(null);

        AppException ex = assertThrows(AppException.class,
                () -> authService.completeRegistration(SetPasswordRequest.builder().registrationToken("invalid").build(), response));

        assertEquals(ErrorCode.INVALID_ONETIME_TOKEN, ex.getErrorCode());
    }

    @Test
    void completeRegistration_PassengerRoleMissing_ThrowsException() {
        when(accountTokenService.getPhoneNumberByRegistrationToken("registration-token")).thenReturn("0900000002");
        when(accountRepository.existsByPhoneNumber("0900000002")).thenReturn(false);
        when(roleRepository.findByName("PASSENGER")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> authService.completeRegistration(SetPasswordRequest.builder()
                        .registrationToken("registration-token")
                        .password("P@ssword123")
                        .build(), response));

        assertEquals(ErrorCode.ROLE_NOT_FOUND, ex.getErrorCode());
    }
}
