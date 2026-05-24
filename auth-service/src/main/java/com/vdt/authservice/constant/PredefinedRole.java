package com.vdt.authservice.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class PredefinedRole {

    // =========================================================================
    // 1. CÁC HẰNG SỐ CHUỖI (Giữ nguyên dùng cho HasRole() hoặc PreAuthorize)
    // =========================================================================
    // TẦNG 1: Quản trị viên tối cao (Hội đồng trung tâm)
    public static final String ADMIN = "ADMIN";

    // TẦNG 2: Quản lý của từng đơn vị vận hành (Ví dụ: Manager của VinBus, Metro Cát Linh)
    public static final String COMPANY_MANAGER = "COMPANY_MANAGER";

    // TẦNG 3: Nhân viên ca kíp tại các nhà ga/quầy vé thuộc công ty đó
    public static final String STAFF = "STAFF";

    // TẦNG 4: Hành khách tham gia giao thông
    public static final String PASSENGER = "PASSENGER";

    // =========================================================================
    // 2. ENUM NỘI BỘ (Phục vụ gom cụm Tên vai trò + Mô tả tiếng Việt để Seed dữ liệu)
    // =========================================================================
    @Getter
    @RequiredArgsConstructor
    public enum Definition {
        SUPER_ADMIN(ADMIN, "Quản trị viên hệ thống - Quản lý cấu hình hệ thống, biểu giá và đối soát tổng"),
        COMP_MANAGER(COMPANY_MANAGER, "Quản lý đơn vị vận hành - Quản lý nhân sự, ca trực và báo cáo doanh thu nội bộ đơn vị"),
        STF(STAFF, "Nhân viên vận hành - Trực ca kíp tại quầy, hỗ trợ hành khách xử lý sự cố thẻ lỗi"),
        PSG(PASSENGER, "Hành khách - Người tham gia giao thông sử dụng dịch vụ thẻ vé liên tuyến");

        private final String name;
        private final String description;
    }

    private PredefinedRole() {}
}