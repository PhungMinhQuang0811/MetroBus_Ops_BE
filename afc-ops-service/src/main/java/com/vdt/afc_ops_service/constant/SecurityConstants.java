package com.vdt.afc_ops_service.constant;

import java.util.Map;
import java.util.Set;

public final class SecurityConstants {

    public static final String ROLE_PREFIX = "ROLE_";
    public static final String ACCOUNT_DISABLED_KEY_PREFIX = "auth:account:disabled:";

    public static final Map<String, String> ENDPOINT_PERMISSIONS = Map.of(
            "/afc-ops/list-routes", PredefinedAfcPermission.MASTER_DATA_READ,
            "/afc-ops/create-route", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/afc-ops/update-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE,
            "/afc-ops/import-routes", PredefinedAfcPermission.MASTER_DATA_WRITE
    );

    public static final Map<String, Set<String>> ROLE_AFC_PERMISSIONS = Map.of(
            ROLE_PREFIX + PredefinedAuthRole.OPERATOR_ADMIN, Set.of(
                    PredefinedAfcPermission.MASTER_DATA_READ,
                    PredefinedAfcPermission.MASTER_DATA_WRITE
            ),
            ROLE_PREFIX + PredefinedAuthRole.OPERATOR_MANAGER, Set.of(
                    PredefinedAfcPermission.MASTER_DATA_READ,
                    PredefinedAfcPermission.MASTER_DATA_WRITE
            ),
            ROLE_PREFIX + PredefinedAuthRole.STATION_OPERATOR, Set.of(
                    PredefinedAfcPermission.MASTER_DATA_READ
            )
    );

    public static final String[] ENDPOINT_THIRD_PARTY = {

    };

    private SecurityConstants() {}
}
