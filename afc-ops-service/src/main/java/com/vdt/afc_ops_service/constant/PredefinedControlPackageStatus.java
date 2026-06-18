package com.vdt.afc_ops_service.constant;

public final class PredefinedControlPackageStatus {

    public static final String CREATED = "CREATED";
    public static final String PUBLISHED = "PUBLISHED";
    public static final String REVOKED = "REVOKED";

    public static boolean isValid(String value) {
        return CREATED.equals(value) || PUBLISHED.equals(value) || REVOKED.equals(value);
    }

    private PredefinedControlPackageStatus() {}
}
