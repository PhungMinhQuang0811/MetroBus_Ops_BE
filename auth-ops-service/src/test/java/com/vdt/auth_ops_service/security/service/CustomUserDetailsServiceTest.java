package com.vdt.auth_ops_service.security.service;

import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Permission;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.security.service.Impl.CustomUserDetailsService;
import com.vdt.auth_ops_service.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private Account mockAccount;

    @BeforeEach
    void setUp() {
        Permission permission = Permission.builder().name("ACCOUNT_READ").build();
        Role role = Role.builder().name("OPERATOR_MANAGER").permissions(Set.of(permission)).build();
        
        mockAccount = Account.builder()
                .id("test-id")
                .username("testuser")
                .password("encoded-password")
                .roles(Set.of(role))
                .isActive(true)
                .build();
    }

    @Test
    void loadUserByUsername_Success() {
        // Given
        when(accountRepository.findByIdentifier("testuser")).thenReturn(Optional.of(mockAccount));

        // When
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("testuser");

        // Then
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encoded-password", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ACCOUNT_READ")));
        
        verify(accountRepository, times(1)).findByIdentifier("testuser");
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        // Given
        when(accountRepository.findByIdentifier("unknown")).thenReturn(Optional.empty());

        // When & Then
        assertThrows(AppException.class, () -> {
            customUserDetailsService.loadUserByUsername("unknown");
        });

        verify(accountRepository, times(1)).findByIdentifier("unknown");
    }
}
