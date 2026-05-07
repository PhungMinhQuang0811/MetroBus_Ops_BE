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
import com.vdt.authservice.util.RedisUtil;
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
    RedisUtil redisUtil;
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

    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        String token = jwtUtil.generateToken(account);
        String refreshToken = jwtUtil.generateRefreshToken(account);
        
        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(accessTokenCookieName, token, "/", accessTokenExpiration, true).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(refreshTokenCookieName, refreshToken, "/", refreshTokenExpiration, true).toString());

        return authMapper.toAuthResponse(account);
    }

    @Transactional
    public void activateAccount(String token) {
        String accountId = accountTokenService.getAccountIdByActivationToken(token);
        if (accountId == null) {
            throw new AppException(ErrorCode.INVALID_ONETIME_TOKEN);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        account.setActive(true);
        account.setEmailVerified(true);
        accountRepository.save(account);
        
        accountTokenService.deleteActivationToken(token);
    }

    public void forgotPassword(String email) {
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_USED_BY_ANY_ACCOUNT));

        String token = accountTokenService.generateResetPasswordToken(account.getId());
        emailService.sendEmail(account.getEmail(), "Reset Password", 
                "Click here to reset: http://localhost:8081/api/v1/auth/reset-password?token=" + token);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String accountId = accountTokenService.getAccountIdByResetPasswordToken(request.getToken());
        if (accountId == null) {
            throw new AppException(ErrorCode.INVALID_ONETIME_TOKEN);
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        
        accountTokenService.deleteResetPasswordToken(request.getToken());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        invalidateTokens(request);

        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(accessTokenCookieName, "", "/", 0, true).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(refreshTokenCookieName, "", "/", 0, true).toString());
    }

    private void invalidateTokens(HttpServletRequest request) {
        String accessToken = getCookieValueByName(request, accessTokenCookieName);
        String refreshToken = getCookieValueByName(request, refreshTokenCookieName);

        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                jwtUtil.verifyAccessToken(accessToken);
                tokenManagementService.invalidateAccessToken(accessToken, jwtUtil.getExpirationAtFromAccessToken(accessToken));
            } catch (Exception e) {
                log.info("Invalid access token, user cannot be authenticated with this access token");
            }
        }

        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                jwtUtil.verifyRefreshToken(refreshToken);
                tokenManagementService.invalidateRefreshToken(refreshToken, jwtUtil.getExpirationAtFromRefreshToken(refreshToken));
            } catch (Exception e) {
                log.info("Invalid refresh token, user cannot refresh access token with this refresh token");
            }
        }
        
        // TODO: Handle CSRF token invalidation when implemented
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
        try {
            SignedJWT signedJWT = jwtUtil.verifyRefreshToken(refreshToken);
            String email = signedJWT.getJWTClaimsSet().getSubject();
            
            Account account = accountRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));
                    
            String newAccessToken = jwtUtil.generateToken(account);
            String newRefreshToken = jwtUtil.generateRefreshToken(account);
            
            response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(accessTokenCookieName, newAccessToken, "/", accessTokenExpiration, true).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, generateCookie(refreshTokenCookieName, newRefreshToken, "/", refreshTokenExpiration, true).toString());
            
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
    private ResponseCookie generateCookie(String cookieName, String cookieValue, String path, long maxAgeMiliseconds, boolean isHttpOnly) {
        return ResponseCookie
                .from(cookieName, cookieValue)
//                .path(path)
                .path("/")
//                .domain(domain)
                .maxAge(maxAgeMiliseconds / 1000) // seconds ~ 1days
                .httpOnly(isHttpOnly)
//                .secure(true)
                .secure(false)
//                .sameSite("None")
                .sameSite("Lax")
                .build();
    }
}

