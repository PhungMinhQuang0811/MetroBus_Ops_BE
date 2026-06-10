package com.vdt.afc_ops_service.constant;

import java.util.Map;

public final class SecurityConstants {

    public static final String ACCOUNT_DISABLED_KEY_PREFIX = "auth:account:disabled:";

    public static final Map<String, String> ENDPOINT_PERMISSIONS = Map.of(
            "/afc-ops/list-routes", PredefinedAfcPermission.MASTER_DATA_READ,
            "/afc-ops/create-route", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/afc-ops/update-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/afc-ops/enable-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/afc-ops/disable-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/afc-ops/sync-operator-manager-role-permissions", PredefinedAfcPermission.MASTER_DATA_WRITE
    );

    public static final String[] ENDPOINT_THIRD_PARTY = {

    };

    private SecurityConstants() {}
}
