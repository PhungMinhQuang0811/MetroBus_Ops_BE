package com.vdt.afc_ops_service.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class PredefinedAfcPermission {

    public static final String MASTER_DATA_READ = "MASTER_DATA_READ";
    public static final String MASTER_DATA_WRITE = "MASTER_DATA_WRITE";

    @Getter
    @RequiredArgsConstructor
    public enum Definition {
        MASTER_DATA_READ(PredefinedAfcPermission.MASTER_DATA_READ, "Xem danh mục master data AFC Cấp 3/Cấp 4"),
        MASTER_DATA_WRITE(PredefinedAfcPermission.MASTER_DATA_WRITE, "Tạo, cập nhật và import danh mục master data AFC Cấp 3/Cấp 4");

        private final String name;
        private final String description;
    }

    private PredefinedAfcPermission() {}
}
