package com.vdt.afc_ops_service.constant;

public final class PredefinedControlPackageType {

    public static final String DEVICE_CONFIG = "DEVICE_CONFIG";
    public static final String MEDIA_ACCESS_RULES = "MEDIA_ACCESS_RULES";

    public static boolean isValid(String value) {
        return DEVICE_CONFIG.equals(value) || MEDIA_ACCESS_RULES.equals(value);
    }

    private PredefinedControlPackageType() {}
}
