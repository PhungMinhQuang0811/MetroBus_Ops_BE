package com.vdt.afc_ops_service.messaging;

import com.vdt.afc_ops_service.config.MessageBrokerConfig;
import com.vdt.afc_ops_service.constant.PredefinedAfcPermission;
import com.vdt.afc_ops_service.constant.PredefinedAuthRole;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthPermissionSyncPublisher {

    RabbitTemplate rabbitTemplate;
    MessageBrokerConfig.AuthPermissionSyncProperties authPermissionSyncProperties;

    public void publishAfcPermissions() {
        Map<String, Object> message = Map.of(
                "source", "afc-ops-service",
                "permissions", Arrays.stream(PredefinedAfcPermission.Definition.values())
                        .map(permission -> Map.of(
                                "name", permission.getName(),
                                "description", permission.getDescription()
                        ))
                        .sorted((left, right) -> String.valueOf(left.get("name"))
                                .compareTo(String.valueOf(right.get("name"))))
                        .toList()
        );

        publish(message);
        log.info("Published AFC permissions sync message");
    }

    public void publishOperatorManagerRolePermissions() {
        Map<String, Object> message = Map.of(
                "source", "afc-ops-service",
                "rolePermissions", List.of(Map.of(
                        "roleName", PredefinedAuthRole.OPERATOR_MANAGER,
                        "permissions", List.of(
                                PredefinedAfcPermission.MASTER_DATA_READ,
                                PredefinedAfcPermission.MASTER_DATA_WRITE
                        )
                ))
        );

        publish(message);
        log.info("Published AFC role permissions sync message for {}", PredefinedAuthRole.OPERATOR_MANAGER);
    }

    private void publish(Map<String, Object> message) {
        rabbitTemplate.convertAndSend(
                authPermissionSyncProperties.exchange(),
                authPermissionSyncProperties.routingKey(),
                message
        );
    }
}
