package com.vdt.afc_ops_service.integration.level5.constant;

import java.util.Set;

public final class PredefinedLevel5BusinessSync {

    public static final String CARD_UPSERT = "CARD_UPSERT";
    public static final String CARD_STATUS_CHANGED = "CARD_STATUS_CHANGED";
    public static final String TICKET_UPSERT = "TICKET_UPSERT";
    public static final String TICKET_STATUS_CHANGED = "TICKET_STATUS_CHANGED";
    public static final String ENTITLEMENT_UPSERT = "ENTITLEMENT_UPSERT";
    public static final String ENTITLEMENT_STATUS_CHANGED = "ENTITLEMENT_STATUS_CHANGED";

    public static final String CREATED = "CREATED";
    public static final String UPDATED = "UPDATED";
    public static final String IGNORED_SAME_VERSION = "IGNORED_SAME_VERSION";
    public static final String IGNORED_STALE_VERSION = "IGNORED_STALE_VERSION";
    public static final String REJECTED = "REJECTED";

    public static final String VIRTUAL_QR = "VIRTUAL_QR";
    public static final String PHYSICAL = "PHYSICAL";

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";
    public static final String CANCELLED = "CANCELLED";
    public static final String BLACKLISTED = "BLACKLISTED";

    public static final String METRO_SINGLE_RIDE = "METRO_SINGLE_RIDE";
    public static final String SINGLE_ROUTE = "SINGLE_ROUTE";
    public static final String NETWORK = "NETWORK";
    public static final String UNUSED = "UNUSED";
    public static final String IN_USE = "IN_USE";
    public static final String USED = "USED";
    public static final String EXPIRED = "EXPIRED";

    public static final String MONTHLY_PASS = "MONTHLY_PASS";
    public static final String MONTH = "MONTH";
    public static final String INTERLINE = "INTERLINE";
    public static final String ALL = "ALL";

    public static final Set<String> SYNC_TYPES = Set.of(
            CARD_UPSERT,
            CARD_STATUS_CHANGED,
            TICKET_UPSERT,
            TICKET_STATUS_CHANGED,
            ENTITLEMENT_UPSERT,
            ENTITLEMENT_STATUS_CHANGED
    );

    public static final Set<String> CARD_TYPES = Set.of(VIRTUAL_QR, PHYSICAL);
    public static final Set<String> CARD_STATUSES = Set.of(ACTIVE, INACTIVE, CANCELLED, BLACKLISTED);
    public static final Set<String> ROUTE_SCOPE_TYPES = Set.of(SINGLE_ROUTE, NETWORK);
    public static final Set<String> TICKET_USAGE_STATUSES = Set.of(UNUSED, IN_USE, USED, EXPIRED, CANCELLED);
    public static final Set<String> ENTITLEMENT_SCOPES = Set.of(SINGLE_ROUTE, INTERLINE, NETWORK);
    public static final Set<String> ENTITLEMENT_STATUSES = Set.of(ACTIVE, INACTIVE, CANCELLED, EXPIRED);

    private PredefinedLevel5BusinessSync() {
    }
}
