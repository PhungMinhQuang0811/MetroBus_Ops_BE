package com.vdt.auth_ops_service.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class PredefinedRole {

    public static final String OPERATOR_ADMIN = "OPERATOR_ADMIN";
    public static final String OPERATOR_MANAGER = "OPERATOR_MANAGER";
    public static final String STATION_OPERATOR = "STATION_OPERATOR";

    @Getter
    @RequiredArgsConstructor
    public enum Definition {
        OPERATOR_ADMIN("Admin của đơn vị vận hành Cấp 4, khởi tạo và quản lý tài khoản nội bộ"),
        OPERATOR_MANAGER("Quản lý vận hành Cấp 4 của một operator"),
        STATION_OPERATOR("Nhân viên hoặc giám sát tại ga/trạm/tuyến Cấp 3");

        private final String description;
    }

    private PredefinedRole() {}
}
