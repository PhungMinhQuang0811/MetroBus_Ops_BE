package com.vdt.auth_ops_service.security.config;



import com.vdt.auth_ops_service.constant.SecurityConstants;
import com.vdt.auth_ops_service.dto.response.ApiResponse;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) 
            throws IOException, ServletException {
        ErrorCode errorCode = resolveErrorCode(request, accessDeniedException);

        response.setStatus(errorCode.getHttpStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
        response.flushBuffer();
    }

    private ErrorCode resolveErrorCode(HttpServletRequest request, AccessDeniedException accessDeniedException) {
        if (!(accessDeniedException instanceof CsrfException)) {
            return ErrorCode.ACCESS_DENIED;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ErrorCode.UNAUTHENTICATED;
        }

        String requiredPermission = getRequiredPermission(request);
        if (requiredPermission != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .noneMatch(requiredPermission::equals)) {
            return ErrorCode.ACCESS_DENIED;
        }

        return ErrorCode.INVALID_CSRF_TOKEN;
    }

    private String getRequiredPermission(HttpServletRequest request) {
        String path = request.getServletPath();
        return SecurityConstants.ENDPOINT_PERMISSIONS.entrySet().stream()
                .filter(entry -> matches(entry.getKey(), path))
                .map(java.util.Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean matches(String pattern, String path) {
        if (pattern.endsWith("/**")) {
            return path.startsWith(pattern.substring(0, pattern.length() - 3));
        }

        return pattern.equals(path);
    }
}
