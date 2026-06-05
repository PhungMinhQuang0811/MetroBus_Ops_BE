package com.vdt.auth_ops_service.security.service.Impl;

import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Permission;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.common.util.RedisUtil;
import com.vdt.auth_ops_service.security.service.IUserPermissionService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserPermissionService implements IUserPermissionService {
    AccountRepository accountRepository;
    RedisUtil redisUtil;

    public static final String PERM_CACHE_PREFIX = "user_perms:";
    static final long PERM_CACHE_TTL = 1; // 1 day

    @Override
    public void clearAllPermissionCache() {
        redisUtil.deleteByPrefix(PERM_CACHE_PREFIX);
    }

    @Transactional(readOnly = true)
    @Override
    public Collection<? extends GrantedAuthority> getUserPermissions(String accountId) {
        String cacheKey = PERM_CACHE_PREFIX + accountId;
        
        // 1. Try to get from Redis
        Set<String> permissions = redisUtil.getSet(cacheKey);
        
        if (CollectionUtils.isEmpty(permissions)) {
            // 2. Cache miss, load from DB
            permissions = loadPermissionsFromDb(accountId);
            
            // 3. Save to Redis
            if (!permissions.isEmpty()) {
                redisUtil.addSet(cacheKey, permissions, PERM_CACHE_TTL, TimeUnit.DAYS);
            }
        }

        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private Set<String> loadPermissionsFromDb(String accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Set<String> permissions = new HashSet<>();

        if (CollectionUtils.isEmpty(account.getRoles())) {
            return permissions;
        }

        //fetch roles
        for (Role role : account.getRoles()) {
            //check role has permission
            if (!CollectionUtils.isEmpty(role.getPermissions())) {
                for (Permission p : role.getPermissions()) {
                    permissions.add(p.getName());
                }
            }
        }

        return permissions;
    }

    @Override
    public void invalidateCache(String accountId) {
        redisUtil.delete(PERM_CACHE_PREFIX + accountId);
    }
}
