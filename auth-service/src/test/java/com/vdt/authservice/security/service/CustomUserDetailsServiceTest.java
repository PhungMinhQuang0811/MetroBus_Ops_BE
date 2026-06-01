package com.vdt.authservice.security.service;

import com.vdt.authservice.modules.identity.entity.Account;
import com.vdt.authservice.modules.identity.entity.Permission;
import com.vdt.authservice.modules.identity.entity.Role;
import com.vdt.authservice.common.exception.AppException;
import com.vdt.authservice.modules.identity.security.service.Impl.CustomUserDetailsService;
import com.vdt.authservice.modules.identity.repository.AccountRepository;
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
        Permission permission = Permission.builder().name("account:read").build();
        Role role = Role.builder().name("USER").permissions(Set.of(permission)).build();
        
        mockAccount = Account.builder()
                .id("test-id")
                .email("test@example.com")
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
                .anyMatch(a -> a.getAuthority().equals("account:read")));
        
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
