package com.vdt.afc_ops_service.security.auth;

import com.nimbusds.jwt.SignedJWT;
import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.SecurityConstants;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import com.vdt.afc_ops_service.security.service.TokenStatusService;
import com.vdt.afc_ops_service.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    JwtUtil jwtUtil;
    TokenStatusService tokenStatusService;
    HandlerExceptionResolver handlerExceptionResolver;

    @NonFinal
    @Value("${app.security.access-token-cookie-name}")
    String accessTokenCookieName;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && !tokenStatusService.isAccessTokenInvalidated(token)) {
            try {
                SignedJWT signedJWT = jwtUtil.verifyAccessToken(token);
                String accountId = signedJWT.getJWTClaimsSet().getSubject();
                String username = signedJWT.getJWTClaimsSet().getStringClaim("username");

                if (tokenStatusService.isAccountDisabled(accountId)) {
                    throw new AppException(ErrorCode.ACCOUNT_DISABLED);
                }

                Collection<SimpleGrantedAuthority> authorities = resolveAuthorities(
                        signedJWT.getJWTClaimsSet().getStringClaim("scope")
                );

                AfcUserDetails userDetails = AfcUserDetails.builder()
                        .id(accountId)
                        .username(username)
                        .authorities(authorities)
                        .build();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (AppException e) {
                SecurityContextHolder.clearContext();
                handlerExceptionResolver.resolveException(request, response, null, e);
                return;
            } catch (ParseException e) {
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

    private String resolveToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> accessTokenCookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private Collection<SimpleGrantedAuthority> resolveAuthorities(String scope) {
        Set<String> authorities = new LinkedHashSet<>();
        if (StringUtils.hasText(scope)) {
            authorities.addAll(Arrays.asList(scope.split("\\s+")));
        }

        Set<String> roleAuthorities = new LinkedHashSet<>(authorities);
        roleAuthorities.forEach(role -> {
            Set<String> afcPermissions = SecurityConstants.ROLE_AFC_PERMISSIONS.get(role);
            if (afcPermissions != null) {
                authorities.addAll(afcPermissions);
            }
        });

        return authorities.stream()
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .toList();
    }
}
