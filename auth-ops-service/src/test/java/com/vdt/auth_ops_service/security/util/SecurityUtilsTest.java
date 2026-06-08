package com.vdt.auth_ops_service.security.util;

import com.vdt.auth_ops_service.security.entity.CustomUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityUtilsTest {

    private SecurityContext securityContext;
    private Authentication authentication;
    private CustomUserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        securityContext = mock(SecurityContext.class);
        authentication = mock(Authentication.class);
        SecurityContextHolder.setContext(securityContext);

        mockUserDetails = CustomUserDetails.builder()
                .id("acc-123")
                .username("testuser")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_OPERATOR_MANAGER"), new SimpleGrantedAuthority("ACCOUNT_READ")))
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUserDetails);

        CustomUserDetails result = SecurityUtils.getCurrentUser();
        assertNotNull(result);
        assertEquals("acc-123", result.getId());
    }

    @Test
    void getCurrentAccountId_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUserDetails);

        assertEquals("acc-123", SecurityUtils.getCurrentAccountId());
    }

    @Test
    void getCurrentUsername_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUserDetails);

        assertEquals("testuser", SecurityUtils.getCurrentUsername());
    }

    @Test
    void getCurrentRoles_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_OPERATOR_ADMIN"), new SimpleGrantedAuthority("ACCOUNT_READ")))
                .when(authentication).getAuthorities();

        List<String> roles = SecurityUtils.getCurrentRoles();
        assertEquals(1, roles.size());
        assertEquals("OPERATOR_ADMIN", roles.get(0));
    }

    @Test
    void getCurrentRoles_NoRoleAuthorities_ReturnsEmptyList() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority("ACCOUNT_READ"), new SimpleGrantedAuthority("ACCOUNT_WRITE")))
                .when(authentication).getAuthorities();

        assertTrue(SecurityUtils.getCurrentRoles().isEmpty());
    }

    @Test
    void getCurrentAuthorities_Success() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_OPERATOR_ADMIN"), new SimpleGrantedAuthority("ACCOUNT_READ")))
                .when(authentication).getAuthorities();

        List<String> authorities = SecurityUtils.getCurrentAuthorities();
        assertEquals(2, authorities.size());
        assertTrue(authorities.contains("ROLE_OPERATOR_ADMIN"));
        assertTrue(authorities.contains("ACCOUNT_READ"));
    }

    @Test
    void isAdmin_ReturnsTrue() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_OPERATOR_ADMIN")))
                .when(authentication).getAuthorities();

        assertTrue(SecurityUtils.isAdmin());
    }

    @Test
    void isAdmin_ReturnsFalse() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_OPERATOR_MANAGER")))
                .when(authentication).getAuthorities();

        assertFalse(SecurityUtils.isAdmin());
    }

    @Test
    void isAuthenticated_ReturnsTrue() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(mockUserDetails);

        assertTrue(SecurityUtils.isAuthenticated());
    }

    @Test
    void getCurrentUser_NoAuth_ReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);
        assertNull(SecurityUtils.getCurrentUser());
    }

    @Test
    void getCurrentUser_NotAuthenticated_ReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        assertNull(SecurityUtils.getCurrentUser());
    }

    @Test
    void getCurrentUser_WrongPrincipalType_ReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("not-custom-user-details");
        assertNull(SecurityUtils.getCurrentUser());
    }

    @Test
    void getCurrentAccountId_NoUser_ReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);
        assertNull(SecurityUtils.getCurrentAccountId());
    }

    @Test
    void getCurrentUsername_NoUser_ReturnsNull() {
        when(securityContext.getAuthentication()).thenReturn(null);
        assertNull(SecurityUtils.getCurrentUsername());
    }

    @Test
    void getCurrentRoles_NoAuth_ReturnsEmptyList() {
        when(securityContext.getAuthentication()).thenReturn(null);
        assertTrue(SecurityUtils.getCurrentRoles().isEmpty());
    }

    @Test
    void getCurrentAuthorities_NoAuth_ReturnsEmptyList() {
        when(securityContext.getAuthentication()).thenReturn(null);
        assertTrue(SecurityUtils.getCurrentAuthorities().isEmpty());
    }

    @Test
    void isAuthenticated_AnonymousUser_ReturnsFalse() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("anonymousUser");
        assertFalse(SecurityUtils.isAuthenticated());
    }

    @Test
    void isAuthenticated_NotAuthenticated_ReturnsFalse() {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);
        assertFalse(SecurityUtils.isAuthenticated());
    }
}
