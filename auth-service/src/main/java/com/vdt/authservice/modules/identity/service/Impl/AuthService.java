package com.vdt.authservice.modules.identity.service.Impl;

import com.nimbusds.jwt.SignedJWT;
import com.vdt.authservice.modules.identity.dto.request.auth.LoginRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.ResetPasswordRequest;
import com.vdt.authservice.modules.identity.dto.request.auth.VerifyOtpRequest;
import com.vdt.authservice.modules.identity.dto.response.auth.AuthResponse;
import com.vdt.authservice.modules.identity.constant.PredefinedRole;
import com.vdt.authservice.modules.identity.entity.Account;
import com.vdt.authservice.common.notification.email.IEmailService;
import com.vdt.authservice.modules.identity.mapper.AuthMapper;
import com.vdt.authservice.modules.identity.repository.AccountRepository;
import com.vdt.authservice.modules.identity.repository.RoleRepository;
import com.vdt.authservice.common.exception.AppException;
import com.vdt.authservice.common.exception.ErrorCode;
import com.vdt.authservice.modules.identity.security.service.ITokenManagementService;
import com.vdt.authservice.modules.identity.security.service.IAccountTokenService;
import com.vdt.authservice.modules.identity.security.util.JwtUtil;
import com.vdt.authservice.modules.identity.service.IAuthService;
import com.vdt.authservice.modules.identity.service.IOtpService;
import com.vdt.authservice.modules.wallet.constant.WalletStatus;
import com.vdt.authservice.modules.wallet.constant.WalletType;
import com.vdt.authservice.modules.wallet.entity.Wallet;
import com.vdt.authservice.modules.wallet.repository.WalletRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService implements IAuthService {
    AccountRepository accountRepository;
    RoleRepository roleRepository;
    WalletRepository walletRepository;
    AuthMapper authMapper;
    PasswordEncoder passwordEncoder;
    AuthenticationManager authenticationManager;
    JwtUtil jwtUtil;
    IAccountTokenService accountTokenService;
    IEmailService emailService;
    ITokenManagementService tokenManagementService;
    IOtpService otpService;

    @NonFinal
    @Value("${app.security.jwt.access-token-expiration}")
    long accessTokenExpiration;

    @NonFinal
    @Value("${app.security.jwt.refresh-token-expiration}")
    long refreshTokenExpiration;

    @NonFinal
    @Value("${app.security.jwt.access-token-cookie-name}")
    String accessTokenCookieName;

    @NonFinal
    @Value("${app.security.jwt.refresh-token-cookie-name}")
    String refreshTokenCookieName;

    @NonFinal
    @Value("${server.servlet.context-path:/}")
    String contextPath;

    @NonFinal
    @Value("${app.security.refresh-path}")
    String refreshPath;

    @NonFinal
    @Value("${app.security.logout-path}")
    String logoutPath;
    
    @NonFinal
    @Value("${app.security.csrf-cookie-name}")
    String csrfCookieName;

    @Value("${app.domain-name}")
    @NonFinal
    String domain;

    @Override
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        Account account = accountRepository.findByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        validateAccountStatus(account);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
        );

        setTokenCookies(response, account);

        return authMapper.toAuthResponse(account);
    }

    @Transactional
    @Override
    public AuthResponse verifyOtp(VerifyOtpRequest request, HttpServletResponse response) {
        String phoneNumber = otpService.normalizePhoneNumber(request.getPhoneNumber());
        otpService.verifyOtp(phoneNumber, request.getOtp());

        Account account = accountRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> createPassengerAccount(phoneNumber));

        validateAccountStatus(account);

        if (!account.isPhoneVerified()) {
            account.setPhoneVerified(true);
            account = accountRepository.save(account);
        }

        ensurePassengerWallet(account);
        setTokenCookies(response, account);

        return authMapper.toAuthResponse(account);
    }

    @Override
    public void forgotPassword(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_USED_BY_ANY_ACCOUNT));

        validateAccountStatus(account);

        String token = accountTokenService.generateResetPasswordToken(account.getId());
        emailService.sendResetPasswordEmail(account.getEmail(), token);
    }

    @Transactional
    @Override
    public void resetPassword(ResetPasswordRequest request) {
        Account account = verifyResetPasswordTokenAndGetAccount(request.getToken());

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        
        accountTokenService.deleteResetPasswordToken(request.getToken());
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        invalidateTokens(request);
        clearTokenCookies(response);
    }

    private void invalidateTokens(HttpServletRequest request) {
        String accessToken = getCookieValueByName(request, accessTokenCookieName);
        String refreshToken = getCookieValueByName(request, refreshTokenCookieName);

        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                jwtUtil.verifyAccessToken(accessToken);
                tokenManagementService.invalidateAccessToken(accessToken, jwtUtil.getExpirationAtFromAccessToken(accessToken));
            } catch (Exception e) {
                throw new AppException(ErrorCode.TOKEN_EXPIRED_OR_INVALID);
            }
        }

        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                jwtUtil.verifyRefreshToken(refreshToken);
                tokenManagementService.invalidateRefreshToken(refreshToken, jwtUtil.getExpirationAtFromRefreshToken(refreshToken));
            } catch (Exception e) {
                throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
            }
        }
    }

    private String getCookieValueByName(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    @Override
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = getCookieValueByName(request, refreshTokenCookieName);
        if (refreshToken == null || tokenManagementService.isRefreshTokenInvalidated(refreshToken)) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        SignedJWT signedJWT;
        try {
            signedJWT = jwtUtil.verifyRefreshToken(refreshToken);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String accountId;
        try {
            accountId = signedJWT.getJWTClaimsSet().getSubject();
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        validateAccountStatus(account);

        setTokenCookies(response, account);
    }
    private void validateAccountStatus(Account account) {
        if (!account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private Account createPassengerAccount(String phoneNumber) {
        var passengerRole = roleRepository.findByName(PredefinedRole.PASSENGER)
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        Account account = Account.builder()
                .phoneNumber(phoneNumber)
                .isActive(true)
                .isPhoneVerified(true)
                .roles(Set.of(passengerRole))
                .build();

        return accountRepository.save(account);
    }


    private void ensurePassengerWallet(Account account) {
        if (walletRepository.existsByAccountIdAndWalletType(account.getId(), WalletType.PASSENGER)) {
            return;
        }

        Wallet wallet = Wallet.builder()
                .account(account)
                .walletType(WalletType.PASSENGER)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();

        walletRepository.save(wallet);
    }

    private void setTokenCookies(HttpServletResponse response, Account account) {
        String accessToken = jwtUtil.generateToken(account);
        String refreshToken = jwtUtil.generateRefreshToken(account);

        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(accessTokenCookieName, accessToken, contextPath, accessTokenExpiration, true).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(refreshTokenCookieName, refreshToken, refreshPath, refreshTokenExpiration, true).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(refreshTokenCookieName, refreshToken, logoutPath, refreshTokenExpiration, true).toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(accessTokenCookieName, "", contextPath, 0, true).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(refreshTokenCookieName, "", refreshPath, 0, true).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(refreshTokenCookieName, "", logoutPath, 0, true).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(csrfCookieName, "", contextPath, 0, false).toString());
    }

    private Account verifyResetPasswordTokenAndGetAccount(String token) {
        String accountId = accountTokenService.getAccountIdByResetPasswordToken(token);
        if (accountId == null) {
            throw new AppException(ErrorCode.INVALID_ONETIME_TOKEN);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        validateAccountStatus(account);
        return account;
    }

    private ResponseCookie generateCookie(String cookieName, String cookieValue, String path, long maxAgeMiliseconds, boolean isHttpOnly) {
        return ResponseCookie
                .from(cookieName, cookieValue)
                .path(path)
                .domain(domain)
                .maxAge(maxAgeMiliseconds / 1000) // seconds ~ 1days
                .httpOnly(isHttpOnly)
                .secure(true)
                .sameSite("None")
                .build();
    }
}
