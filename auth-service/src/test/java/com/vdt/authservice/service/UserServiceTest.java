package com.vdt.authservice.service;

import com.vdt.authservice.dto.request.user.RegisterRequest;
import com.vdt.authservice.dto.response.user.UserResponse;
import com.vdt.authservice.entity.Account;
import com.vdt.authservice.exception.AppException;
import com.vdt.authservice.exception.ErrorCode;
import com.vdt.authservice.external.notification.email.EmailService;
import com.vdt.authservice.mapper.UserMapper;
import com.vdt.authservice.repository.AccountRepository;
import com.vdt.authservice.repository.RoleRepository;
import com.vdt.authservice.security.service.AccountTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccountTokenService accountTokenService;
    @Mock private EmailService emailService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private Account mockAccount;

    @BeforeEach
    void setUp() {
        mockAccount = Account.builder()
                .id("acc-123")
                .email("test@example.com")
                .username("testuser")
                .isEmailVerified(false)
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest req = RegisterRequest.builder()
                .email("test@example.com").username("testuser").password("pwd").roles(Set.of("USER")).build();
        when(accountRepository.existsByEmail(any())).thenReturn(false);
        when(accountRepository.existsByUsername(any())).thenReturn(false);
        when(accountRepository.save(any())).thenReturn(mockAccount);
        // Dùng any() thay cho anyString() để tránh lỗi nếu getId() bị null trong lúc mock
        when(accountTokenService.generateActivationToken(any())).thenReturn("token-123");
        when(userMapper.toUserResponse(any())).thenReturn(new UserResponse());

        assertNotNull(userService.register(req));
        verify(emailService).sendActivationEmail(eq("test@example.com"), eq("token-123"));
    }

    @Test
    void activateAccount_InvalidToken_ThrowsException() {
        when(accountTokenService.getAccountIdByActivationToken("invalid")).thenReturn(null);
        AppException ex = assertThrows(AppException.class, () -> userService.activateAccount("invalid"));
        assertEquals(ErrorCode.INVALID_ONETIME_TOKEN, ex.getErrorCode());
    }

    @Test
    void resendActivationEmail_Success() {
        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockAccount));
        when(accountTokenService.getExistingActivationToken(mockAccount.getId())).thenReturn("old-token");

        userService.resendActivationEmail("test@example.com");

        verify(emailService).sendActivationEmail(eq("test@example.com"), eq("old-token"));
    }

    @Test
    void register_UsernameExisted_ThrowsException() {
        when(accountRepository.existsByEmail(any())).thenReturn(false);
        when(accountRepository.existsByUsername(any())).thenReturn(true);
        
        RegisterRequest req = RegisterRequest.builder()
                .email("test@example.com")
                .username("existed")
                .build();
                
        AppException ex = assertThrows(AppException.class, () -> userService.register(req));
        assertEquals(ErrorCode.USER_EXISTED, ex.getErrorCode());
    }

    @Test
    void activateAccount_Success() {
        // Given
        String token = "valid-token";
        String accountId = "acc-123";
        
        // Đảm bảo mockAccount ở trạng thái chưa active trước khi test
        mockAccount.setActive(false);
        mockAccount.setEmailVerified(false);
        
        when(accountTokenService.getAccountIdByActivationToken(token)).thenReturn(accountId);
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any())).thenReturn(mockAccount);

        // When
        userService.activateAccount(token);

        // Then
        verify(accountRepository, times(1)).findById(accountId);
        verify(accountRepository, times(1)).save(mockAccount);
        verify(accountTokenService, times(1)).deleteActivationToken(token);
        
        assertTrue(mockAccount.isActive(), "Account should be active");
        assertTrue(mockAccount.isEmailVerified(), "Email should be verified");
    }

    @Test
    void activateAccount_AccountNotFound_ThrowsException() {
        when(accountTokenService.getAccountIdByActivationToken("token")).thenReturn("acc-id");
        when(accountRepository.findById("acc-id")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> userService.activateAccount("token"));
    }

    @Test
    void resendActivationEmail_UserNotFound_ThrowsException() {
        when(accountRepository.findByEmail("no@example.com")).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> userService.resendActivationEmail("no@example.com"));
    }

    @Test
    void resendActivationEmail_TokenExpired_GeneratesNewToken() {
        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockAccount));
        when(accountTokenService.getExistingActivationToken(mockAccount.getId())).thenReturn(null);
        when(accountTokenService.generateActivationToken(mockAccount.getId())).thenReturn("new-token");

        userService.resendActivationEmail("test@example.com");

        verify(accountTokenService).generateActivationToken(mockAccount.getId());
        verify(emailService).sendActivationEmail(eq("test@example.com"), eq("new-token"));
    }
    @Test
    void resendActivationEmail_AlreadyVerified_ThrowsException() {
        mockAccount.setEmailVerified(true);
        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(mockAccount));

        AppException ex = assertThrows(AppException.class, () -> userService.resendActivationEmail("test@example.com"));
        assertEquals(ErrorCode.USER_ALREADY_VERIFIED, ex.getErrorCode());
    }

    @Test
    void getAllUsers_Success() {
        when(accountRepository.findAll()).thenReturn(java.util.List.of(mockAccount));
        when(userMapper.toUserResponse(any())).thenReturn(new UserResponse());

        java.util.List<UserResponse> result = userService.getAllUsers();
        assertEquals(1, result.size());
    }
}
