package com.vdt.afc_ops_service.constant;

import java.util.Map;

public final class SecurityConstants {

    public static final String ACCOUNT_DISABLED_KEY_PREFIX = "auth:account:disabled:";

    public static final Map<String, String> ENDPOINT_PERMISSIONS = Map.of(
            "/route/list-routes", PredefinedAfcPermission.MASTER_DATA_READ,
            "/route/create-route", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/route/update-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/route/enable-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/route/disable-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/route/preview-import-routes", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/route/confirm-import-routes", PredefinedAfcPermission.MASTER_DATA_WRITE
    );

    public static final String[] ENDPOINT_THIRD_PARTY = {

    };

    private SecurityConstants() {}
}
