package com.vdt.afc_ops_service.config;

import com.vdt.afc_ops_service.messaging.AuthPermissionSyncPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    AuthPermissionSyncPublisher authPermissionSyncPublisher;

    @NonFinal
    @Value("${app.init.enabled}")
    boolean initEnabled;

    @NonFinal
    @Value("${app.init.sync-auth-permissions-enabled}")
    boolean syncAuthPermissionsEnabled;

    @Bean
    ApplicationRunner initializeOperators() {
        return args -> {
            if (!initEnabled) {
                return;
            }

            if (syncAuthPermissionsEnabled) {
                authPermissionSyncPublisher.publishAfcPermissions();
            }
        };
    }
}

