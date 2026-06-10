package com.vdt.auth_ops_service.config;

import com.vdt.auth_ops_service.constant.PredefinedPasswordStatus;
import com.vdt.auth_ops_service.constant.PredefinedPermission;
import com.vdt.auth_ops_service.constant.PredefinedRole;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Permission;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.repository.PermissionRepository;
import com.vdt.auth_ops_service.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
@ConditionalOnProperty(prefix = "app.init", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    @NonFinal
    @Value("${app.init.admin.password}")
    String adminPassword;

    static final String DEFAULT_OPERATOR_CODE = "HCMC-METRO";
    String adminUsername = "admin";

    @Bean
    ApplicationRunner applicationRunner(AccountRepository accountRepository,
                                        RoleRepository roleRepository,
                                        PermissionRepository permissionRepository,
                                        PasswordEncoder passwordEncoder) {
        return args -> {
            // =========================================================================
            // BƯỚC 1: KHỞI TẠO DANH MỤC PERMISSIONS (Reflection + Enum mô tả)
            // =========================================================================
            log.info("Checking and initializing missing permissions...");
            Field[] fields = PredefinedPermission.class.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.getType() == String.class) {
                    try {
                        String permissionName = (String) field.get(null);

                        String description = null;
                        for (PredefinedPermission.Definition permissionDefinition : PredefinedPermission.Definition.values()) {
                            if (permissionDefinition.getName().equals(permissionName)) {
                                description = permissionDefinition.getDescription();
                            }
                        }

                        if (description == null) {
                            description = "Auto-generated permission: " + permissionName;
                        }

                        createPermission(permissionRepository, permissionName, description);
                    } catch (IllegalAccessException e) {
                        log.error("Failed to read permission constant", e);
                    }
                }
            }

            // =========================================================================
            // BƯỚC 2: KHỞI TẠO VÀ ĐỒNG BỘ DANH MỤC ROLES (Quét cuốn chiếu từng Role)
            // =========================================================================
            log.info("Checking and initializing missing roles...");

            // Gán toàn bộ quyền auth-ops cho OPERATOR_ADMIN mặc định.
            Set<Permission> adminPermissions = new HashSet<>();
            for (PredefinedPermission.Definition permissionDefinition : PredefinedPermission.Definition.values()) {
                Permission savedPerm = permissionRepository.findByName(permissionDefinition.getName()).orElse(null);
                if (savedPerm != null) {
                    adminPermissions.add(savedPerm);
                }
            }

            // Duyệt cuốn chiếu qua từng định nghĩa Role, thiếu ông nào đắp ông đấy ngay lập tức
            for (PredefinedRole.Definition roleDefinition : PredefinedRole.Definition.values()) {
                Set<Permission> rolePermissions;

                if (roleDefinition.name().equals(PredefinedRole.OPERATOR_ADMIN)) {
                    rolePermissions = adminPermissions;
                } else {
                    rolePermissions = new HashSet<>();
                }

                // Gọi hàm bọc kiểm tra: Chưa có dưới DB thì mới tạo mới
                createRole(roleRepository, roleDefinition.name(), roleDefinition.getDescription(), rolePermissions);
            }

            // =========================================================================
            // BƯỚC 3: KHỞI TẠO OPERATOR_ADMIN CHO TỪNG OPERATOR SEED
            // =========================================================================
            Role adminRole = roleRepository.findByName(PredefinedRole.OPERATOR_ADMIN).orElse(null);
            if (adminRole == null) {
                log.error("Failed to initialize system: OPERATOR_ADMIN role could not be found in Database.");
                return;
            }

            for (String operatorCode : getOperatorSeeds()) {
                String adminUsernameForOperator = getAdminUsername(operatorCode);
                if (accountRepository.existsByUsername(adminUsernameForOperator)) {
                    continue;
                }

                log.info("Initializing operator admin account {} for {}", adminUsernameForOperator, operatorCode);
                Account adminAccount = Account.builder()
                        .username(adminUsernameForOperator)
                        .operatorCode(operatorCode)
                        .password(passwordEncoder.encode(adminPassword))
                        .isActive(true)
                        .passwordStatus(PredefinedPasswordStatus.NORMAL)
                        .roles(Set.of(adminRole))
                        .build();

                accountRepository.save(adminAccount);
            }
        };
    }

    private void createPermission(PermissionRepository repository, String name, String description) {
        repository.findByName(name).orElseGet(() -> {
            Permission p = Permission.builder().name(name).description(description).build();
            return repository.save(p);
        });
    }

    // Thêm hàm bọc kiểm tra Role tương đương với hàm createPermission của bạn
    private void createRole(RoleRepository repository, String name, String description, Set<Permission> permissions) {
        repository.findByName(name).orElseGet(() -> {
            Role r = Role.builder()
                    .name(name)
                    .description(description)
                    .permissions(permissions)
                    .build();
            return repository.save(r);
        });
    }

    private Set<String> getOperatorSeeds() {
        Set<String> operatorCodes = new LinkedHashSet<>();
        addOperatorSeed(operatorCodes, DEFAULT_OPERATOR_CODE);
        addOperatorSeed(operatorCodes, "HCMC-METRO");
        addOperatorSeed(operatorCodes, "HCMC-BUS");
        addOperatorSeed(operatorCodes, "HCMC-BRT");
        return operatorCodes;
    }

    private void addOperatorSeed(Set<String> operatorCodes, String operatorCode) {
        if (operatorCode != null && !operatorCode.isBlank()) {
            operatorCodes.add(operatorCode.trim());
        }
    }

    private String getAdminUsername(String operatorCode) {
        if (operatorCode.equals(DEFAULT_OPERATOR_CODE)) {
            return adminUsername;
        }
        return "admin_" + operatorCode.toLowerCase().replace('-', '_');
    }

}
