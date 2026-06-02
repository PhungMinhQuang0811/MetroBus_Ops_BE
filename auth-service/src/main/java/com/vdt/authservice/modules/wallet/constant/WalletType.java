package com.vdt.authservice.modules.wallet.constant;

public class WalletType {

    // 1. Các loại ví dành cho Doanh nghiệp vận hành (Operator)
    public static final String OPERATOR_REVENUE = "OPERATOR_REVENUE";     // Ví Doanh thu (Ghi sổ thu hộ)
    public static final String OPERATOR_DEPOSIT = "OPERATOR_DEPOSIT";     // Ví Ký quỹ (Đóng băng bảo lãnh)
    public static final String OPERATOR_OPERATING = "OPERATOR_OPERATING"; // Ví Vận hành (Chi phí, hoàn tiền)

    // 2. Ví dành cho Cơ quan quản lý hệ thống trung tâm (Platform - Sở GTVT)
    public static final String PLATFORM = "PLATFORM";

    // 3. Ví dành cho Admin
    public static final String ADMIN = "ADMIN";


    private WalletType() {}
}