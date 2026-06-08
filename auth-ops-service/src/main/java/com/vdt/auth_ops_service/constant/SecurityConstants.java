package com.vdt.auth_ops_service.constant;

import java.util.Map;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final Map<String, String> ENDPOINT_PERMISSIONS = Map.of(
            "/account/list-accounts", PredefinedPermission.ACCOUNT_READ,
            "/account/get-account/**", PredefinedPermission.ACCOUNT_READ,
            "/account/create-account", PredefinedPermission.ACCOUNT_WRITE,
            "/account/disable-account/**", PredefinedPermission.ACCOUNT_WRITE,
            "/account/enable-account/**", PredefinedPermission.ACCOUNT_WRITE,
            "/account/preview-import-accounts", PredefinedPermission.ACCOUNT_WRITE,
            "/account/confirm-import-accounts", PredefinedPermission.ACCOUNT_WRITE,
            "/account/reset-account-password/**", PredefinedPermission.ACCOUNT_WRITE,
            "/auth/search-audit-logs", PredefinedPermission.AUDIT_READ
    );

    public static final String[] ENDPOINT_THIRD_PARTY = {

    };
}
