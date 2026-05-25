package com.vdt.authservice.modules.identity.constant;

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
        ACCOUNT_READ("account:read", "Admin: Xem danh sách và thông tin chi tiết toàn bộ tài khoản"),
        ACCOUNT_ACTIVATE("activate:account", "Mở khóa hoặc kích hoạt lại trạng thái hoạt động của tài khoản"),
        ACCOUNT_DEACTIVATE("account:deactivate", "Vô hiệu hóa hoặc tạm khóa (Ban) trạng thái tài khoản người dùng"),

        SYSTEM_LOG_READ("system:log:read", "Tra cứu hệ thống nhật ký vết (Audit Logs) để thanh tra vận hành"),
        ROLE_MANAGE("role:manage", "Quản trị danh mục cấu hình các Vai trò (Roles)"),
        PERMISSION_MANAGE("permission:manage", "Quản trị danh mục cấu hình các Quyền hạn chi tiết (Permissions)");

        // Bạn giữ lại trường "name" để lưu chuỗi có dấu ":" phục vụ việc lưu xuống DB và check quyền
        private final String name;
        private final String description;
    }

    private PredefinedPermission() {}
}