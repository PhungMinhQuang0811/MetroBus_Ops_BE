package com.vdt.auth_ops_service.messaging;

import com.vdt.auth_ops_service.entity.Permission;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.repository.PermissionRepository;
import com.vdt.auth_ops_service.repository.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthPermissionSyncListener {

    PermissionRepository permissionRepository;
    RoleRepository roleRepository;

    @Transactional
    @RabbitListener(queues = "${app.message-broker.auth-permission-sync.queue}")
    public void syncAfcPermissions(Map<String, Object> message) {
        for (Map<String, Object> permission : mapList(message.get("permissions"))) {
            upsertPermission(permission);
        }

        for (Map<String, Object> rolePermission : mapList(message.get("rolePermissions"))) {
            syncRolePermissions(rolePermission);
        }

        log.info("Synced AFC permission message from {}", message.get("source"));
    }

    private void upsertPermission(Map<String, Object> permissionMessage) {
        String name = stringValue(permissionMessage.get("name"));
        String description = stringValue(permissionMessage.get("description"));
        if (name == null || description == null) {
            return;
        }

        Permission permission = permissionRepository.findByName(name)
                .orElseGet(() -> Permission.builder()
                        .name(name)
                        .description(description)
                        .build());

        if (!Objects.equals(permission.getDescription(), description)) {
            permission.setDescription(description);
        }

        permissionRepository.save(permission);
    }

    private void syncRolePermissions(Map<String, Object> rolePermissionMessage) {
        String roleName = stringValue(rolePermissionMessage.get("roleName"));
        if (roleName == null) {
            return;
        }

        Role role = roleRepository.findByName(roleName).orElse(null);
        if (role == null) {
            log.warn("Skip syncing AFC permissions for missing auth role {}", roleName);
            return;
        }

        if (role.getPermissions() == null) {
            role.setPermissions(new HashSet<>());
        }

        for (String permissionName : stringList(rolePermissionMessage.get("permissions"))) {
            Permission permission = permissionRepository.findByName(permissionName).orElse(null);
            if (permission != null) {
                role.getPermissions().add(permission);
            }
        }

        roleRepository.save(role);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        return values.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> values)) {
            return List.of();
        }

        return values.stream()
                .map(AuthPermissionSyncListener::stringValue)
                .filter(Objects::nonNull)
                .toList();
    }

    private static String stringValue(Object value) {
        return value instanceof String text && !text.isBlank() ? text : null;
    }
}
