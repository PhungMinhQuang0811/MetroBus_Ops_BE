package com.vdt.afc_ops_service.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class PredefinedAfcPermission {

    public static final String MASTER_DATA_READ = "MASTER_DATA_READ";
    public static final String MASTER_DATA_WRITE = "MASTER_DATA_WRITE";
    public static final String TRANSACTION_READ = "TRANSACTION_READ";
    public static final String BATCH_READ = "BATCH_READ";
    public static final String BATCH_WRITE = "BATCH_WRITE";

    @Getter
    @RequiredArgsConstructor
    public enum Definition {
        MASTER_DATA_READ(PredefinedAfcPermission.MASTER_DATA_READ, "Xem danh mục master data AFC Cấp 3/Cấp 4"),
        MASTER_DATA_WRITE(PredefinedAfcPermission.MASTER_DATA_WRITE, "Tạo, cập nhật và import danh mục master data AFC Cấp 3/Cấp 4"),
        TRANSACTION_READ(PredefinedAfcPermission.TRANSACTION_READ, "Tra cứu transaction vận hành AFC Cấp 3/Cấp 4"),
        BATCH_READ(PredefinedAfcPermission.BATCH_READ, "Xem batch dữ liệu vận hành gửi Cấp 5"),
        BATCH_WRITE(PredefinedAfcPermission.BATCH_WRITE, "Tạo và gửi batch dữ liệu vận hành lên Cấp 5");

        private final String name;
        private final String description;
    }

    private PredefinedAfcPermission() {}
}
