package com.vdt.afc_ops_service.constant;

import java.util.Map;

import static java.util.Map.entry;

public final class SecurityConstants {

    public static final String ACCOUNT_DISABLED_KEY_PREFIX = "auth:account:disabled:";

    public static final Map<String, String> ENDPOINT_PERMISSIONS = Map.ofEntries(
            entry("/route/list-routes", PredefinedAfcPermission.MASTER_DATA_READ),
            entry("/route/get-route/**", PredefinedAfcPermission.MASTER_DATA_READ),
            entry("/route/create-route", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/route/update-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/route/enable-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/route/disable-route/**", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/route/preview-import-routes", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/route/confirm-import-routes", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/station/list-stations", PredefinedAfcPermission.MASTER_DATA_READ),
            entry("/station/get-station/**", PredefinedAfcPermission.MASTER_DATA_READ),
            entry("/station/create-station", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/station/update-station/**", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/station/enable-station/**", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/station/disable-station/**", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/station/preview-import-stations", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/station/confirm-import-stations", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/device/list-devices", PredefinedAfcPermission.DEVICE_READ),
            entry("/device/get-device/**", PredefinedAfcPermission.DEVICE_READ),
            entry("/device/create-device", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/device/update-device/**", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/device/enable-device/**", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/device/disable-device/**", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/device/preview-import-devices", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/device/confirm-import-devices", PredefinedAfcPermission.MASTER_DATA_WRITE),
            entry("/search-transactions", PredefinedAfcPermission.TRANSACTION_READ),
            entry("/get-transaction-detail", PredefinedAfcPermission.TRANSACTION_READ),
            entry("/batch/create-batch", PredefinedAfcPermission.BATCH_WRITE),
            entry("/batch/list-batches", PredefinedAfcPermission.BATCH_READ),
            entry("/dashboard/**", PredefinedAfcPermission.DASHBOARD_READ),
            entry("/afc-ops/get-device-status", PredefinedAfcPermission.DEVICE_MONITOR_READ),
            entry("/afc-ops/get-device-heartbeats", PredefinedAfcPermission.DEVICE_MONITOR_READ),
            entry("/afc-ops/search-incidents", PredefinedAfcPermission.INCIDENT_READ),
            entry("/afc-ops/get-incident/**", PredefinedAfcPermission.INCIDENT_READ),
            entry("/control-package/create", PredefinedAfcPermission.CONTROL_PACKAGE_WRITE),
            entry("/control-package/update/**", PredefinedAfcPermission.CONTROL_PACKAGE_WRITE),
            entry("/control-package/get-detail", PredefinedAfcPermission.CONTROL_PACKAGE_READ),
            entry("/control-package/list", PredefinedAfcPermission.CONTROL_PACKAGE_READ),
            entry("/control-package/publish/**", PredefinedAfcPermission.CONTROL_PACKAGE_WRITE),
            entry("/control-package/search-syncs", PredefinedAfcPermission.CONTROL_PACKAGE_READ),
            entry("/control-package/get-sync-detail", PredefinedAfcPermission.CONTROL_PACKAGE_READ)
    );

    public static final String[] ENDPOINT_THIRD_PARTY = {
            "/generate-dynamic-qr",
            "/submit-tap-event",
            "/afc-ops/submit-heartbeat",
            "/afc-ops/submit-device-incident",
            "/control-package/pull-pending",
            "/control-package/ack-apply/**"
    };

    private SecurityConstants() {}
}
