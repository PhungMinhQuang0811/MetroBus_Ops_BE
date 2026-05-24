package com.vdt.authservice.constant;

import java.util.Map;

public final class SecurityConstants {

    private SecurityConstants() {}

    public static final Map<String, String> ENDPOINT_PERMISSIONS = Map.of(
            "/test/permissions", PredefinedPermission.PERMISSION_MANAGE,
            "/test/deactivate-account", PredefinedPermission.ACCOUNT_DEACTIVATE,
            "/test/activate-account", PredefinedPermission.ACCOUNT_ACTIVATE,
            "/test/add-permission", PredefinedPermission.PERMISSION_MANAGE,
            "/test/update-permission/**", PredefinedPermission.PERMISSION_MANAGE,
            "/test/add-permission-to-role", PredefinedPermission.PERMISSION_MANAGE,
            "/test/add-role-to-user", PredefinedPermission.ROLE_MANAGE
//            "/test/add-permission-to-me", PredefinedPermission.PERMISSION_WRITE
    );

    public static final String[] ENDPOINT_THIRD_PARTY = {

    };
}
