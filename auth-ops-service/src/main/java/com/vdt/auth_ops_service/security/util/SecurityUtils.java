package com.vdt.auth_ops_service.security.util;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.common.util.SearchFilterUtil;
import com.vdt.auth_ops_service.security.entity.CustomUserDetails;
import com.vdt.auth_ops_service.constant.PredefinedRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SecurityUtils {

    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return (CustomUserDetails) principal;
        }
        return null;
    }

    public static String getCurrentAccountId() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public static String getCurrentUsername() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    public static String getCurrentOperatorCode() {
        CustomUserDetails user = getCurrentUser();
        return user != null ? user.getOperatorCode() : null;
    }

    public static String getRequiredCurrentOperatorCode() {
        String operatorCode = SearchFilterUtil.normalize(getCurrentOperatorCode());
        if (operatorCode == null) {
            throw new AppException(ErrorCode.OPERATOR_SCOPE_REQUIRED);
        }
        return operatorCode;
    }

    /**
     * Lấy danh sách Role của người dùng hiện tại.
     */
    public static List<String> getCurrentRoles() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(auth -> auth.startsWith("ROLE_"))
                .map(auth -> auth.substring(5)) // Bỏ tiền tố ROLE_
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả quyền hạn (bao gồm cả ROLE_ và Permission).
     */
    public static List<String> getCurrentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }

    /**
     * Kiểm tra người dùng hiện tại có phải là OPERATOR_ADMIN không.
     */
    public static boolean isAdmin() {
        return getCurrentRoles().contains(PredefinedRole.OPERATOR_ADMIN);
    }

    /**
     * Kiểm tra xem đã đăng nhập chưa.
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() 
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
