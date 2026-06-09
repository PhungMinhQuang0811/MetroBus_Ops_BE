package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.response.ApiResponse;
import com.vdt.afc_ops_service.messaging.AuthPermissionSyncPublisher;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthPermissionSyncController {

    AuthPermissionSyncPublisher authPermissionSyncPublisher;

    @PostMapping("/sync-operator-manager-role-permissions")
    public ApiResponse<Void> syncOperatorManagerRolePermissions() {
        authPermissionSyncPublisher.publishOperatorManagerRolePermissions();
        return ApiResponse.<Void>builder().build();
    }
}
