package com.vdt.authservice.controller;

import com.vdt.authservice.constant.PredefinedPermission;
import com.vdt.authservice.entity.Account;
import com.vdt.authservice.entity.Permission;
import com.vdt.authservice.entity.Role;
import com.vdt.authservice.exception.AppException;
import com.vdt.authservice.exception.ErrorCode;
import com.vdt.authservice.repository.AccountRepository;
import com.vdt.authservice.repository.PermissionRepository;
import com.vdt.authservice.repository.RoleRepository;
import com.vdt.authservice.security.entity.CustomUserDetails;
import com.vdt.authservice.security.service.UserPermissionService;
import com.vdt.authservice.security.util.SecurityUtils;
import com.vdt.authservice.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final AccountRepository accountRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RedisUtil redisUtil;
    private final UserPermissionService userPermissionService;

    @GetMapping("/permissions")
    public List<String> getMyPermissions() {
        return SecurityUtils.getCurrentAuthorities();
    }

    @PostMapping("/create-account")
    public String createAccount(@RequestBody CreateAccountRequest request) {
        Account account = Account.builder()
                .email(request.getEmail())
                .username(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .isEmailVerified(true)
                .build();
        accountRepository.save(account);
        return "Account created successfully: " + request.getEmail();
    }

    @PatchMapping("/change-password")
    public String changePassword(@RequestBody ChangePasswordRequest request) {
        String accountId = SecurityUtils.getCurrentAccountId();
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        
        return "Password changed for user: " + SecurityUtils.getCurrentEmail();
    }
    
    @PatchMapping("/deactivate-account")
    public String deactivateAccount(@RequestBody AccountActionRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        account.setActive(false);
        accountRepository.save(account);
        return "Account deactivated: " + request.getAccountId();
    }

    @PostMapping("/activate-account")
    public String activateAccount(@RequestBody AccountActionRequest request) {
        Account account = accountRepository.findById(request.getAccountId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        account.setActive(true);
        accountRepository.save(account);
        return "Account activated: " + request.getAccountId();
    }

    @PostMapping("/add-permission")
    public String addPermission(@RequestBody PermissionRequest request) {
        Permission permission = Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        permissionRepository.save(permission);
        
        userPermissionService.clearAllPermissionCache();
        
        return "Permission added successfully: " + request.getName();
    }

    @PatchMapping("/update-permission/{id}")
    public String updatePermission(@PathVariable int id, @RequestBody PermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        
        String oldName = permission.getName();
        permission.setName(request.getName());
        permission.setDescription(request.getDescription());
        permissionRepository.save(permission);
        
        userPermissionService.clearAllPermissionCache();
        
        return "Permission updated successfully: " + request.getName();
    }

    @PostMapping("/add-permission-to-role")
    public String addPermissionToRole(@RequestBody AddPermissionToRoleRequest request) {
        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        Permission permission = permissionRepository.findByName(request.getPermissionName())
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        
        role.getPermissions().add(permission);
        roleRepository.save(role);
        
        // Many users can have this role, so clear all to be safe
        userPermissionService.clearAllPermissionCache();
        
        return "Permission " + request.getPermissionName() + " added to role " + request.getRoleName();
    }

    @PostMapping("/add-role-to-user")
    public String addRoleToUser(@RequestBody AddRoleToUserRequest request) {
        // Dynamic test: Add role to MYSELF using SecurityUtils
        String accountId = SecurityUtils.getCurrentAccountId();
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Role role = roleRepository.findByName(request.getRoleName())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));
        
        account.getRoles().add(role);
        accountRepository.save(account);
        
        // Dynamic: Clear cache for myself
        userPermissionService.invalidateCache(accountId);
        
        return "Role " + request.getRoleName() + " added to YOUR account (ID: " + accountId + ")";
    }

    @PostMapping("/add-permission-to-me")
    public String addPermissionToMyRoles(@RequestBody AddPermissionToUserRequest request) {
        String accountId = SecurityUtils.getCurrentAccountId();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        Permission permission = permissionRepository.findByName(request.getPermissionName())
                .orElseThrow(() -> new AppException(ErrorCode.PERMISSION_NOT_FOUND));
        
        if (account.getRoles().isEmpty()) {
            return "You don't have any roles to add permission to!";
        }

        for (Role role : account.getRoles()) {
            role.getPermissions().add(permission);
            roleRepository.save(role);
        }
        
        userPermissionService.clearAllPermissionCache();
        return "Permission " + request.getPermissionName() + " added to all YOUR roles (" + account.getRoles().size() + " roles)";
    }

    @lombok.Data
    public static class CreateAccountRequest {
        private String email;
        private String password;
    }

    @lombok.Data
    public static class ChangePasswordRequest {
        private String newPassword;
    }

    @lombok.Data
    public static class AccountActionRequest {
        private String accountId;
    }

    @lombok.Data
    public static class AddPermissionToRoleRequest {
        private String roleName;
        private String permissionName;
    }

    @lombok.Data
    public static class AddRoleToUserRequest {
        private String roleName;
    }

    @lombok.Data
    public static class AddPermissionToUserRequest {
        private String permissionName;
    }

    @lombok.Data
    public static class PermissionRequest {
        private String name;
        private String description;
    }
}
