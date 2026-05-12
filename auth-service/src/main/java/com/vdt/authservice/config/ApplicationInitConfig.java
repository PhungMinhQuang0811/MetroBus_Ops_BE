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
import java.util.stream.Collectors;

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
                        createPermission(permissionRepository, permissionName, "Auto-generated permission: " + permissionName);
                    } catch (IllegalAccessException e) {
                        log.error("Failed to read permission constant", e);
                    }
                }
            }

            if (roleRepository.count() == 0 && accountRepository.count() == 0) {
                log.info("Initializing default roles and admin account...");

                // 1. Get all Permissions to assign to Admin
                Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());

                // 2. Create Roles
                Role adminRole = Role.builder()
                        .name(PredefinedRole.ADMIN)
                        .description("Administrator Role")
                        .permissions(allPermissions)
                        .build();

                roleRepository.save(adminRole);

                // 3. Create Admin Account
                Account adminAccount = Account.builder()
                        .email(adminEmail)
                        .username("admin")
                        .password(passwordEncoder.encode(adminPassword))
                        .isActive(true)
                        .isEmailVerified(true)
                        .roles(Set.of(adminRole))
                        .build();
                
                accountRepository.save(adminAccount);

                log.info("Initialization completed successfully.");
            }
        };
    }

    private Permission createPermission(PermissionRepository repository, String name, String description) {
        return repository.findByName(name).orElseGet(() -> {
            Permission p = Permission.builder().name(name).description(description).build();
            return repository.save(p);
        });
    }
}
