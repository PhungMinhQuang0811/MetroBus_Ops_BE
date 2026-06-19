package com.vdt.afc_ops_service.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class PredefinedAfcPermission {

    public static final String MASTER_DATA_READ = "MASTER_DATA_READ";
    public static final String MASTER_DATA_WRITE = "MASTER_DATA_WRITE";
    public static final String DEVICE_READ = "DEVICE_READ";
    public static final String TRANSACTION_READ = "TRANSACTION_READ";
    public static final String BATCH_READ = "BATCH_READ";
    public static final String BATCH_WRITE = "BATCH_WRITE";
    public static final String DEVICE_MONITOR_READ = "DEVICE_MONITOR_READ";
    public static final String INCIDENT_READ = "INCIDENT_READ";
    public static final String CONTROL_PACKAGE_READ = "CONTROL_PACKAGE_READ";
    public static final String CONTROL_PACKAGE_WRITE = "CONTROL_PACKAGE_WRITE";
    public static final String DASHBOARD_READ = "DASHBOARD_READ";
    public static final String AUDIT_READ = "AUDIT_READ";

    @Getter
    @RequiredArgsConstructor
    public enum Definition {
        MASTER_DATA_READ(PredefinedAfcPermission.MASTER_DATA_READ, "Xem danh mục master data AFC Cấp 3/Cấp 4"),
        MASTER_DATA_WRITE(PredefinedAfcPermission.MASTER_DATA_WRITE, "Tạo, cập nhật và import danh mục master data AFC Cấp 3/Cấp 4"),
        DEVICE_READ(PredefinedAfcPermission.DEVICE_READ, "Tra cứu thông tin thiết bị AFC Cấp 3/Cấp 4"),
        TRANSACTION_READ(PredefinedAfcPermission.TRANSACTION_READ, "Tra cứu transaction vận hành AFC Cấp 3/Cấp 4"),
        BATCH_READ(PredefinedAfcPermission.BATCH_READ, "Xem batch dữ liệu vận hành gửi Cấp 5"),
        BATCH_WRITE(PredefinedAfcPermission.BATCH_WRITE, "Tạo và gửi batch dữ liệu vận hành lên Cấp 5"),
        DEVICE_MONITOR_READ(PredefinedAfcPermission.DEVICE_MONITOR_READ, "Theo dõi trạng thái hoạt động thiết bị"),
        INCIDENT_READ(PredefinedAfcPermission.INCIDENT_READ, "Theo dõi incident thiết bị"),
        CONTROL_PACKAGE_READ(PredefinedAfcPermission.CONTROL_PACKAGE_READ, "Xem gói cấu hình vận hành AFC"),
        CONTROL_PACKAGE_WRITE(PredefinedAfcPermission.CONTROL_PACKAGE_WRITE, "Tạo, cập nhật và phát hành gói cấu hình vận hành AFC"),
        DASHBOARD_READ(PredefinedAfcPermission.DASHBOARD_READ, "Xem dashboard vận hành AFC"),
        AUDIT_READ(PredefinedAfcPermission.AUDIT_READ, "Xem audit log vận hành AFC");

        private final String name;
        private final String description;
    }

    private PredefinedAfcPermission() {}
}
