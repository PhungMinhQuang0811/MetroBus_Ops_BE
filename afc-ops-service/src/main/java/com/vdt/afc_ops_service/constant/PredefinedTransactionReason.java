package com.vdt.afc_ops_service.constant;

public final class PredefinedTransactionReason {

    public static final String VALID = "VALID";
    public static final String DEVICE_DISABLED = "DEVICE_DISABLED";
    public static final String INVALID_DIRECTION = "INVALID_DIRECTION";
    public static final String UNKNOWN_MEDIA = "UNKNOWN_MEDIA";
    public static final String QR_EXPIRED = "QR_EXPIRED";
    public static final String QR_INVALID = "QR_INVALID";
    public static final String QR_REPLAYED = "QR_REPLAYED";
    public static final String MEDIA_BLACKLISTED = "MEDIA_BLACKLISTED";
    public static final String CARD_INACTIVE = "CARD_INACTIVE";
    public static final String CARD_CANCELLED = "CARD_CANCELLED";
    public static final String ACTIVE_PRODUCT_NOT_FOUND = "ACTIVE_PRODUCT_NOT_FOUND";
    public static final String ACTIVE_PRODUCT_CONFLICT = "ACTIVE_PRODUCT_CONFLICT";
    public static final String ENTITLEMENT_EXPIRED = "ENTITLEMENT_EXPIRED";
    public static final String ENTITLEMENT_INACTIVE = "ENTITLEMENT_INACTIVE";
    public static final String TICKET_INVALID = "TICKET_INVALID";
    public static final String TICKET_EXPIRED = "TICKET_EXPIRED";
    public static final String TICKET_ALREADY_USED = "TICKET_ALREADY_USED";

    private PredefinedTransactionReason() {
    }
}
