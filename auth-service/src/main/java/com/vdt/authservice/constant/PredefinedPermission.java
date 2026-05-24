package com.vdt.authservice.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class PredefinedPermission {

    // =========================================================================
    // CÁC HẰNG SỐ CHUỖI TẬP TRUNG CHO QUẢN TRỊ TÀI KHOẢN (Identity Management)
    // =========================================================================
    public static final String ACCOUNT_READ = "account:read";
    public static final String ACCOUNT_ACTIVATE = "account:activate";
    public static final String ACCOUNT_DEACTIVATE = "account:deactivate";


    public static final String SYSTEM_LOG_READ = "system:log:read";
    public static final String ROLE_MANAGE = "role:manage";
    public static final String PERMISSION_MANAGE = "permission:manage";

    // =========================================================================
    // ENUM NỘI BỘ (Phục vụ Data Seeding hệ thống bảo mật nền tảng)
    // =========================================================================
    @Getter
    @RequiredArgsConstructor
    public enum Definition {
        ACT_READ(ACCOUNT_READ, "Admin: Xem danh sách và thông tin chi tiết toàn bộ tài khoản"),
        ACT_ACT(ACCOUNT_ACTIVATE, "Mở khóa hoặc kích hoạt lại trạng thái hoạt động của tài khoản"),
        ACT_DEA(ACCOUNT_DEACTIVATE, "Vô hiệu hóa hoặc tạm khóa (Ban) trạng thái tài khoản người dùng"),

        SYS_LOG(SYSTEM_LOG_READ, "Tra cứu hệ thống nhật ký vết (Audit Logs) để thanh tra vận hành"),
        ROL_MNG(ROLE_MANAGE, "Quản trị danh mục cấu hình các Vai trò (Roles)"),
        PRM_MNG(PERMISSION_MANAGE, "Quản trị danh mục cấu hình các Quyền hạn chi tiết (Permissions)");

        private final String name;
        private final String description;
    }

    private PredefinedPermission() {}
}