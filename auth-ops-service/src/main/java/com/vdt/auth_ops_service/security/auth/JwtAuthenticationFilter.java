package com.vdt.auth_ops_service.security.auth;

import com.nimbusds.jwt.SignedJWT;
import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.constant.PredefinedPasswordStatus;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.security.entity.CustomUserDetails;
import com.vdt.auth_ops_service.security.service.ITokenManagementService;
import com.vdt.auth_ops_service.security.service.IUserPermissionService;
import com.vdt.auth_ops_service.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.experimental.NonFinal;

import java.io.IOException;
import java.util.Arrays;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    JwtUtil jwtUtil;
    ITokenManagementService tokenManagementService;
    IUserPermissionService userPermissionService;
    AccountRepository accountRepository;
    HandlerExceptionResolver handlerExceptionResolver;

    @NonFinal
    @Value("${app.security.jwt.access-token-cookie-name}")
    String accessTokenCookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String token = null;
        if (request.getCookies() != null) {
            token = Arrays.stream(request.getCookies())
                    .filter(cookie -> accessTokenCookieName.equals(cookie.getName()))
                    .map(Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }

        if (token != null && !tokenManagementService.isAccessTokenInvalidated(token)) {
            try {
                SignedJWT signedJWT = jwtUtil.verifyAccessToken(token);
                String userId = signedJWT.getJWTClaimsSet().getSubject();
                String username = signedJWT.getJWTClaimsSet().getStringClaim("username");
                Account account = accountRepository.findById(userId)
                        .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));

                if (!account.isActive()) {
                    throw new AppException(ErrorCode.ACCOUNT_DISABLED);
                }

                if (PredefinedPasswordStatus.NEED_TO_CHANGE.equals(account.getPasswordStatus())
                        && !isPasswordChangeAllowedEndpoint(request)) {
                    throw new AppException(ErrorCode.PASSWORD_CHANGE_REQUIRED);
                }

                var authorities = userPermissionService.getUserPermissions(userId);

                CustomUserDetails userDetails = CustomUserDetails.builder()
                        .id(userId)
                        .username(username)
                        .operatorCode(account.getOperatorCode())
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authentication = 
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AppException e) {
                SecurityContextHolder.clearContext();
                handlerExceptionResolver.resolveException(request, response, null, e);
                return;
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                handlerExceptionResolver.resolveException(
                        request,
                        response,
                        null,
                        new AppException(ErrorCode.UNAUTHENTICATED)
                );
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPasswordChangeAllowedEndpoint(HttpServletRequest request) {
        String path = request.getServletPath();
        return "/auth/login".equals(path)
                || "/auth/logout".equals(path)
                || "/account/change-password".equals(path);
    }
}
