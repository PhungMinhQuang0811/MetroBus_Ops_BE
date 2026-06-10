package com.vdt.afc_ops_service.security.util;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityUtils {

    OperatorRepository operatorRepository;

    public static AfcUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AfcUserDetails userDetails) {
            return userDetails;
        }

        return null;
    }

    public static String getCurrentAccountId() {
        AfcUserDetails user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    public static String getRequiredCurrentAccountId() {
        String accountId = SearchFilterUtil.normalize(getCurrentAccountId());
        if (accountId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return accountId;
    }

    public static String getCurrentUsername() {
        AfcUserDetails user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    public static String getCurrentOperatorCode() {
        AfcUserDetails user = getCurrentUser();
        return user != null ? user.getOperatorCode() : null;
    }

    public static String getRequiredCurrentOperatorCode() {
        String operatorCode = SearchFilterUtil.normalize(getCurrentOperatorCode());
        if (operatorCode == null) {
            throw new AppException(ErrorCode.OPERATOR_SCOPE_REQUIRED);
        }
        return operatorCode;
    }

    public Operator getRequiredCurrentOperator() {
        return operatorRepository.findByOperatorCode(getRequiredCurrentOperatorCode())
                .orElseThrow(() -> new AppException(ErrorCode.OPERATOR_NOT_FOUND));
    }

    public static List<String> getCurrentAuthorities() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return List.of();
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());
    }
}
