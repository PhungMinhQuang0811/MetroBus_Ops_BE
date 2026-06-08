package com.vdt.auth_ops_service.service.Impl;

import com.nimbusds.jwt.SignedJWT;
import com.vdt.auth_ops_service.constant.PredefinedPasswordStatus;
import com.vdt.auth_ops_service.dto.request.auth.LoginRequest;
import com.vdt.auth_ops_service.dto.response.auth.AuthResponse;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.mapper.AuthMapper;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.security.service.ITokenManagementService;
import com.vdt.auth_ops_service.security.util.JwtUtil;
import com.vdt.auth_ops_service.service.IAuthService;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthService implements IAuthService {
    AccountRepository accountRepository;
    AuthMapper authMapper;
    AuthenticationManager authenticationManager;
    JwtUtil jwtUtil;
    ITokenManagementService tokenManagementService;

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
        Account account = accountRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        validateAccountStatus(account);

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        validatePasswordStatus(account);
        setTokenCookies(response, account);

        return authMapper.toAuthResponse(account);
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
        validatePasswordStatus(account);

        setTokenCookies(response, account);
    }
    private void validateAccountStatus(Account account) {
        if (!account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }
    }

    private void validatePasswordStatus(Account account) {
        if (PredefinedPasswordStatus.NEED_TO_RESET.equals(account.getPasswordStatus())) {
            throw new AppException(ErrorCode.PASSWORD_RESET_REQUIRED);
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
