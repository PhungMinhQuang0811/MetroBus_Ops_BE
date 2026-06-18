package com.vdt.afc_ops_service.constant;

public final class PredefinedControlSyncStatus {

    public static final String PENDING = "PENDING";
    public static final String APPLIED = "APPLIED";
    public static final String FAILED = "FAILED";

    public static boolean isValidAckStatus(String value) {
        return APPLIED.equals(value) || FAILED.equals(value);
    }

    public static boolean isValidSearchStatus(String value) {
        return PENDING.equals(value) || APPLIED.equals(value) || FAILED.equals(value);
    }

    private PredefinedControlSyncStatus() {}
}
