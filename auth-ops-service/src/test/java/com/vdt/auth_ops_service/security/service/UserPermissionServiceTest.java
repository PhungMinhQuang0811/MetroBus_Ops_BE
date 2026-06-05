package com.vdt.auth_ops_service.security.service;

import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Permission;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.security.service.Impl.UserPermissionService;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.common.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPermissionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private UserPermissionService userPermissionService;

    private String accountId = "user-123";
    private String cacheKey = "user_perms:user-123";

    @Test
    void getUserPermissions_CacheHit_ReturnsFromRedis() {
        // Given
        Set<String> mockPermissions = Set.of("ACCOUNT_READ", "ACCOUNT_WRITE");
        when(redisUtil.getSet(cacheKey)).thenReturn(mockPermissions);

        // When
        Collection<? extends GrantedAuthority> authorities = userPermissionService.getUserPermissions(accountId);

        // Then
        assertEquals(2, authorities.size());
        assertTrue(authorities.stream().anyMatch(a -> a.getAuthority().equals("ACCOUNT_READ")));
        
        verify(redisUtil, times(1)).getSet(cacheKey);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void getUserPermissions_CacheMiss_LoadsFromDbAndSavesToRedis() {
        // Given
        when(redisUtil.getSet(cacheKey)).thenReturn(Set.of()); // Cache miss
        
        Permission p1 = Permission.builder().name("ACCOUNT_READ").build();
        Role role = Role.builder().name("OPERATOR_ADMIN").permissions(Set.of(p1)).build();
        Account account = Account.builder().id(accountId).roles(Set.of(role)).build();
        
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // When
        Collection<? extends GrantedAuthority> authorities = userPermissionService.getUserPermissions(accountId);

        // Then
        assertEquals(1, authorities.size());
        assertEquals("ACCOUNT_READ", authorities.iterator().next().getAuthority());
        
        verify(redisUtil, times(1)).getSet(cacheKey);
        verify(accountRepository, times(1)).findById(accountId);
        verify(redisUtil, times(1)).addSet(eq(cacheKey), anyCollection(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void getUserPermissions_NoPermissions_ReturnsEmptyListAndDoesNotSaveToRedis() {
        // Given
        when(redisUtil.getSet(cacheKey)).thenReturn(Set.of());
        Account account = Account.builder().id(accountId).roles(Set.of()).build(); // No roles
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        // When
        Collection<? extends GrantedAuthority> authorities = userPermissionService.getUserPermissions(accountId);

        // Then
        assertTrue(authorities.isEmpty());
        verify(redisUtil, never()).addSet(eq(cacheKey), anyCollection(), anyLong(), any());
    }
    @Test
    void invalidateCache_Success() {
        // When
        userPermissionService.invalidateCache(accountId);

        // Then
        verify(redisUtil, times(1)).delete(cacheKey);
    }

    @Test
    void clearAllPermissionCache_Success() {
        // When
        userPermissionService.clearAllPermissionCache();

        // Then
        verify(redisUtil, times(1)).deleteByPrefix(UserPermissionService.PERM_CACHE_PREFIX);
    }
}
