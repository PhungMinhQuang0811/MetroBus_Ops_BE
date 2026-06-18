package com.vdt.afc_ops_service.constant;

public final class PredefinedControlPackageSourceType {

    public static final String LEVEL4_CREATED = "LEVEL4_CREATED";
    public static final String LEVEL5_SYNCED = "LEVEL5_SYNCED";

    public static boolean isValid(String value) {
        return LEVEL4_CREATED.equals(value) || LEVEL5_SYNCED.equals(value);
    }

    private PredefinedControlPackageSourceType() {}
}
