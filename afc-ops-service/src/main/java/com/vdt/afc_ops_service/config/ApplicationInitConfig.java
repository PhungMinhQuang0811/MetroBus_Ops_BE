package com.vdt.afc_ops_service.config;

import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.messaging.AuthPermissionSyncPublisher;
import com.vdt.afc_ops_service.repository.OperatorRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    OperatorRepository operatorRepository;
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

            initializePredefinedOperators();
            if (syncAuthPermissionsEnabled) {
                authPermissionSyncPublisher.publishAfcPermissions();
            }
        };
    }

    private void initializePredefinedOperators() {
        List<OperatorSeed> seeds = List.of(
                new OperatorSeed("HCMC-METRO", "Ho Chi Minh City Metro"),
                new OperatorSeed("HCMC-BUS", "Ho Chi Minh City Bus"),
                new OperatorSeed("HCMC-BRT", "Ho Chi Minh City BRT")
        );

        for (OperatorSeed seed : seeds) {
            operatorRepository.findByOperatorCode(seed.operatorCode())
                    .orElseGet(() -> {
                        log.info("Initializing operator {}", seed.operatorCode());
                        return operatorRepository.save(Operator.builder()
                                .operatorCode(seed.operatorCode())
                                .operatorName(seed.operatorName())
                                .status(PredefinedMasterDataStatus.ACTIVE)
                                .build());
                    });
        }
    }

    private record OperatorSeed(String operatorCode, String operatorName) {}
}
