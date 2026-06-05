package com.vdt.auth_ops_service.constant;

import java.util.Map;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final Map<String, String> ENDPOINT_PERMISSIONS = Map.of(
            "/auth/list-accounts", PredefinedPermission.ACCOUNT_READ,
            "/auth/get-account/**", PredefinedPermission.ACCOUNT_READ,
            "/auth/create-account", PredefinedPermission.ACCOUNT_WRITE,
            "/auth/update-account/**", PredefinedPermission.ACCOUNT_WRITE,
            "/auth/disable-account/**", PredefinedPermission.ACCOUNT_WRITE,
            "/auth/enable-account/**", PredefinedPermission.ACCOUNT_WRITE,
            "/auth/reset-account-password/**", PredefinedPermission.ACCOUNT_WRITE,
            "/auth/search-audit-logs", PredefinedPermission.AUDIT_READ
    );

    public static final String[] ENDPOINT_THIRD_PARTY = {

    };
}
