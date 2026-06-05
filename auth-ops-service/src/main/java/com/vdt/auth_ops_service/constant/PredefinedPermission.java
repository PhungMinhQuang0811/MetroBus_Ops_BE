package com.vdt.auth_ops_service.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class PredefinedPermission {

    public static final String ACCOUNT_READ = "ACCOUNT_READ";
    public static final String ACCOUNT_WRITE = "ACCOUNT_WRITE";
    public static final String AUDIT_READ = "AUDIT_READ";

    @Getter
    @RequiredArgsConstructor
    public enum Definition {
        ACCOUNT_READ(PredefinedPermission.ACCOUNT_READ, "Xem danh sách và chi tiết tài khoản nhân sự nội bộ"),
        ACCOUNT_WRITE(PredefinedPermission.ACCOUNT_WRITE, "Tạo, cập nhật, khóa/mở khóa và reset mật khẩu tài khoản nhân sự"),
        AUDIT_READ(PredefinedPermission.AUDIT_READ, "Tra cứu audit log và truy vết thao tác auth-ops");

        private final String name;
        private final String description;
    }

    private PredefinedPermission() {}
}
