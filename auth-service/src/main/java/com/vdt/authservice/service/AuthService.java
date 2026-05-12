package com.vdt.authservice.service;

import com.nimbusds.jwt.SignedJWT;
import com.vdt.authservice.dto.request.auth.LoginRequest;
import com.vdt.authservice.dto.request.auth.ResetPasswordRequest;
import com.vdt.authservice.dto.response.auth.AuthResponse;
import com.vdt.authservice.entity.Account;
import com.vdt.authservice.external.notification.email.EmailService;
import com.vdt.authservice.mapper.AuthMapper;
import com.vdt.authservice.repository.AccountRepository;
import com.vdt.authservice.exception.AppException;
import com.vdt.authservice.exception.ErrorCode;
import com.vdt.authservice.security.service.TokenManagementService;
import com.vdt.authservice.security.service.AccountTokenService;
import com.vdt.authservice.security.util.JwtUtil;
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
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService {
    AccountRepository accountRepository;
    AuthMapper authMapper;
    PasswordEncoder passwordEncoder;
    AuthenticationManager authenticationManager;
    JwtUtil jwtUtil;
    AccountTokenService accountTokenService;
    EmailService emailService;
    TokenManagementService tokenManagementService;

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

    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse response) {
        Account account = accountRepository.findByIdentifier(request.getIdentifier())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        validateAccountStatus(account);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getIdentifier(), request.getPassword())
        );

        CsrfToken csrfToken = (CsrfToken) httpRequest.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }

        setTokenCookies(response, account);

        return authMapper.toAuthResponse(account);
    }

    public void forgotPassword(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_USED_BY_ANY_ACCOUNT));

        validateAccountStatus(account);

        String token = accountTokenService.generateResetPasswordToken(account.getId());
        emailService.sendResetPasswordEmail(account.getEmail(), token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Account account = verifyResetPasswordTokenAndGetAccount(request.getToken());

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        
        accountTokenService.deleteResetPasswordToken(request.getToken());
    }

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
        if (!account.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (!account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }
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

