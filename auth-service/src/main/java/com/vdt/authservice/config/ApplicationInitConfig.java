package com.vdt.authservice.config;

import com.vdt.authservice.constant.PredefinedPermission;
import com.vdt.authservice.constant.PredefinedRole;
import com.vdt.authservice.entity.Account;
import com.vdt.authservice.entity.Permission;
import com.vdt.authservice.entity.Role;
import com.vdt.authservice.repository.AccountRepository;
import com.vdt.authservice.repository.PermissionRepository;
import com.vdt.authservice.repository.RoleRepository;
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
import java.util.Set;

@Configuration
@ConditionalOnProperty(prefix = "app.init", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    @NonFinal
    @Value("${app.init.admin.email}")
    String adminEmail;

    @NonFinal
    @Value("${app.init.admin.password}")
    String adminPassword;

    String adminUsername = "admin";

    @Bean
    ApplicationRunner applicationRunner(AccountRepository accountRepository,
                                        RoleRepository roleRepository,
                                        PermissionRepository permissionRepository,
                                        PasswordEncoder passwordEncoder) {
        return args -> {
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

            if (roleRepository.count() == 0 && accountRepository.count() == 0) {
                log.info("Initializing default roles and admin account...");

                // --- ĐOẠN SỬA ĐỔI CHÍNH XÁC: CHỈ LẤY ĐÚNG CÁC QUYỀN ĐÃ ĐỊNH NGHĨA TRONG ENUM ---
                Set<Permission> adminPermissions = new HashSet<>();

                for (PredefinedPermission.Definition permissionDefinition : PredefinedPermission.Definition.values()) {
                    // Chủ động tìm kiếm thực thể Permission dưới DB bằng chính xác chuỗi định danh (Name)
                    Permission savedPerm = permissionRepository.findByName(permissionDefinition.getName()).orElse(null);
                    if (savedPerm != null) {
                        adminPermissions.add(savedPerm);
                    }
                }
                // -----------------------------------------------------------------------------

                // Khởi tạo danh mục các Role nền tảng cho hệ thống
                Role adminRole = null;

                for (PredefinedRole.Definition roleDefinition : PredefinedRole.Definition.values()) {
                    Set<Permission> rolePermissions;

                    // Nếu đúng là vai trò ADMIN thì mới đắp bộ quyền Identity tối giản vừa lọc ở trên vào
                    if (roleDefinition.getName().equals(PredefinedRole.ADMIN)) {
                        rolePermissions = adminPermissions; // <--- Chỉ gán đúng các quyền Auth cơ bản, tuyệt đối không gán All bừa bãi
                    } else {
                        rolePermissions = new HashSet<>(); // Các role khác để trống quyền để cấu hình động sau
                    }

                    Role role = Role.builder()
                            .name(roleDefinition.getName())
                            .description(roleDefinition.getDescription())
                            .permissions(rolePermissions)
                            .build();

                    Role savedRole = roleRepository.save(role);

                    if (roleDefinition.getName().equals(PredefinedRole.ADMIN)) {
                        adminRole = savedRole;
                    }
                }

                // Khởi tạo DUY NHẤT 1 tài khoản Admin tối cao của hệ thống
                if (adminRole != null) {
                    Account adminAccount = Account.builder()
                            .email(adminEmail)
                            .username(adminUsername)
                            .password(passwordEncoder.encode(adminPassword))
                            .isActive(true)
                            .isEmailVerified(true)
                            .roles(Set.of(adminRole))
                            .build();

                    accountRepository.save(adminAccount);
                    log.info("Initialization completed successfully. Admin account created.");
                } else {
                    log.error("Failed to initialize system: Admin role could not be found.");
                }
            }
        };
    }

    private void createPermission(PermissionRepository repository, String name, String description) {
        repository.findByName(name).orElseGet(() -> {
            Permission p = Permission.builder().name(name).description(description).build();
            return repository.save(p);
        });
    }
}