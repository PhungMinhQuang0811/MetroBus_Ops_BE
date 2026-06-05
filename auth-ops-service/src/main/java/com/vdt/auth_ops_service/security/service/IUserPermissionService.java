package com.vdt.auth_ops_service.security.service;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface IUserPermissionService {
    void clearAllPermissionCache();
    Collection<? extends GrantedAuthority> getUserPermissions(String accountId);
    void invalidateCache(String accountId);
}
