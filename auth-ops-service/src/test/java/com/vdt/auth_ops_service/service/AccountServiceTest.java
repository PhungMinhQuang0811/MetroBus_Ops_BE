package com.vdt.auth_ops_service.service;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.constant.PredefinedPasswordStatus;
import com.vdt.auth_ops_service.constant.PredefinedRole;
import com.vdt.auth_ops_service.dto.request.account.ChangePasswordRequest;
import com.vdt.auth_ops_service.dto.request.account.CreateAccountRequest;
import com.vdt.auth_ops_service.dto.request.account.ResetAccountPasswordRequest;
import com.vdt.auth_ops_service.dto.response.PageResponse;
import com.vdt.auth_ops_service.dto.response.account.AccountResponse;
import com.vdt.auth_ops_service.dto.response.account.ChangePasswordResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountConfirmResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountPreviewResponse;
import com.vdt.auth_ops_service.dto.response.account.ResetAccountPasswordResponse;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.mapper.AccountMapper;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.repository.RoleRepository;
import com.vdt.auth_ops_service.security.entity.CustomUserDetails;
import com.vdt.auth_ops_service.security.service.AccountStatusRedisService;
import com.vdt.auth_ops_service.service.Impl.AccountImportService;
import com.vdt.auth_ops_service.service.Impl.AccountService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {
    @Mock private AccountRepository accountRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private AccountMapper accountMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccountImportService accountImportService;
    @Mock private AccountStatusRedisService accountStatusRedisService;
    @Mock private MultipartFile file;

    @InjectMocks
    private AccountService accountService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listAccounts_Success_NormalizesFilters() {
        Account account = account("operator", PredefinedRole.STATION_OPERATOR, true);
        AccountResponse accountResponse = AccountResponse.builder().username("operator").build();

        when(roleRepository.existsByName(PredefinedRole.STATION_OPERATOR)).thenReturn(true);
        when(accountRepository.searchAccounts(eq("HCMC-METRO"), eq("%operator%"), eq(PredefinedRole.STATION_OPERATOR), eq(true), eq(PredefinedPasswordStatus.NEED_TO_RESET), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(account)));
        when(accountMapper.toAccountResponse(account)).thenReturn(accountResponse);
        authenticateAs(UUID.randomUUID().toString());

        PageResponse<AccountResponse> response = accountService.listAccounts(
                " operator ",
                PredefinedRole.STATION_OPERATOR,
                true,
                PredefinedPasswordStatus.NEED_TO_RESET,
                0,
                20
        );

        assertEquals(1, response.getItems().size());
        assertEquals("operator", response.getItems().getFirst().getUsername());
    }

    @Test
    void listAccounts_BlankFilters_SearchesWithoutKeywordOrRole() {
        when(accountRepository.searchAccounts(eq("HCMC-METRO"), eq("%"), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        authenticateAs(UUID.randomUUID().toString());

        PageResponse<AccountResponse> response = accountService.listAccounts(" ", " ", null, " ", 0, 20);

        assertTrue(response.getItems().isEmpty());
        verify(roleRepository, never()).existsByName(anyString());
    }

    @Test
    void listAccounts_InvalidPage_ThrowsException() {
        AppException exception = assertThrows(AppException.class,
                () -> accountService.listAccounts(null, null, null, null, -1, 20));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, exception.getErrorCode());
    }

    @Test
    void listAccounts_InvalidSize_ThrowsException() {
        AppException tooSmall = assertThrows(AppException.class,
                () -> accountService.listAccounts(null, null, null, null, 0, 0));
        AppException tooLarge = assertThrows(AppException.class,
                () -> accountService.listAccounts(null, null, null, null, 0, 101));

        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, tooSmall.getErrorCode());
        assertEquals(ErrorCode.INVALID_PAGE_REQUEST, tooLarge.getErrorCode());
    }

    @Test
    void listAccounts_InvalidKeyword_ThrowsException() {
        AppException exception = assertThrows(AppException.class,
                () -> accountService.listAccounts("a".repeat(51), null, null, null, 0, 20));

        assertEquals(ErrorCode.INVALID_SEARCH_KEYWORD, exception.getErrorCode());
    }

    @Test
    void listAccounts_UnknownRole_ThrowsException() {
        when(roleRepository.existsByName("UNKNOWN")).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> accountService.listAccounts(null, "UNKNOWN", null, null, 0, 20));

        assertEquals(ErrorCode.INVALID_ROLE_SELECTION, exception.getErrorCode());
    }

    @Test
    void listAccounts_InvalidPasswordStatus_ThrowsException() {
        AppException exception = assertThrows(AppException.class,
                () -> accountService.listAccounts(null, null, null, "INVALID", 0, 20));

        assertEquals(ErrorCode.INVALID_PASSWORD_STATUS, exception.getErrorCode());
    }

    @Test
    void getAccount_InvalidId_ThrowsException() {
        AppException exception = assertThrows(AppException.class, () -> accountService.getAccount("not-uuid"));

        assertEquals(ErrorCode.INVALID_ACCOUNT_ID, exception.getErrorCode());
    }

    @Test
    void getAccount_BlankId_ThrowsException() {
        AppException nullId = assertThrows(AppException.class, () -> accountService.getAccount(null));
        AppException blankId = assertThrows(AppException.class, () -> accountService.getAccount(" "));

        assertEquals(ErrorCode.INVALID_ACCOUNT_ID, nullId.getErrorCode());
        assertEquals(ErrorCode.INVALID_ACCOUNT_ID, blankId.getErrorCode());
    }

    @Test
    void getAccount_Success() {
        String accountId = UUID.randomUUID().toString();
        Account account = account("station01", PredefinedRole.STATION_OPERATOR, true);
        AccountResponse expected = AccountResponse.builder().username("station01").build();

        when(accountRepository.findByIdAndOperatorCode(accountId, "HCMC-METRO")).thenReturn(Optional.of(account));
        when(accountMapper.toAccountResponse(account)).thenReturn(expected);
        authenticateAs(UUID.randomUUID().toString());

        assertSame(expected, accountService.getAccount(accountId));
    }

    @Test
    void getAccount_NotFound_ThrowsException() {
        String accountId = UUID.randomUUID().toString();
        when(accountRepository.findByIdAndOperatorCode(accountId, "HCMC-METRO")).thenReturn(Optional.empty());
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class, () -> accountService.getAccount(accountId));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getAccount_DifferentOperator_ThrowsOperatorAccessDenied() {
        String accountId = UUID.randomUUID().toString();
        when(accountRepository.findByIdAndOperatorCode(accountId, "HCMC-METRO")).thenReturn(Optional.empty());
        when(accountRepository.existsById(accountId)).thenReturn(true);
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class, () -> accountService.getAccount(accountId));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
    }

    @Test
    void createAccount_Success_GeneratesTemporaryPassword() {
        Role role = role(PredefinedRole.STATION_OPERATOR);
        CreateAccountRequest request = CreateAccountRequest.builder()
                .username("station01")
                .roleNames(Set.of(PredefinedRole.STATION_OPERATOR))
                .build();

        when(accountRepository.existsByUsername("station01")).thenReturn(false);
        when(roleRepository.findAllByNameIn(request.getRoleNames())).thenReturn(Set.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(accountMapper.toAccountResponse(any(Account.class))).thenReturn(AccountResponse.builder().username("station01").build());
        authenticateAs(UUID.randomUUID().toString());

        AccountResponse response = accountService.createAccount(request);

        assertNotNull(response.getTemporaryPassword());
        assertEquals(9, response.getTemporaryPassword().length());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertTrue(savedAccount.isActive());
        assertEquals("HCMC-METRO", savedAccount.getOperatorCode());
        assertEquals(PredefinedPasswordStatus.NEED_TO_CHANGE, savedAccount.getPasswordStatus());
        assertEquals(Set.of(role), savedAccount.getRoles());
    }

    @Test
    void createAccount_ExistingUsername_ThrowsException() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .username("station01")
                .roleNames(Set.of(PredefinedRole.STATION_OPERATOR))
                .build();
        when(accountRepository.existsByUsername("station01")).thenReturn(true);
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class, () -> accountService.createAccount(request));

        assertEquals(ErrorCode.USER_EXISTED, exception.getErrorCode());
    }

    @Test
    void createAccount_RoleMissingInDb_ThrowsException() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .username("station01")
                .roleNames(Set.of(PredefinedRole.STATION_OPERATOR))
                .build();

        when(accountRepository.existsByUsername("station01")).thenReturn(false);
        when(roleRepository.findAllByNameIn(request.getRoleNames())).thenReturn(Set.of());
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class, () -> accountService.createAccount(request));

        assertEquals(ErrorCode.INVALID_ROLE_SELECTION, exception.getErrorCode());
    }

    @Test
    void disableAccount_Success() {
        String accountId = UUID.randomUUID().toString();
        Account account = account("station01", PredefinedRole.STATION_OPERATOR, true);
        AccountResponse mapped = AccountResponse.builder().isActive(false).build();

        when(accountRepository.findByIdAndOperatorCode(accountId, "HCMC-METRO")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toAccountResponse(account)).thenReturn(mapped);
        authenticateAs(UUID.randomUUID().toString());

        AccountResponse response = accountService.disableAccount(accountId);

        assertFalse(account.isActive());
        assertFalse(response.getIsActive());
        verify(accountStatusRedisService).markDisabled(accountId);
    }

    @Test
    void disableAccount_Admin_ThrowsException() {
        String accountId = UUID.randomUUID().toString();
        when(accountRepository.findByIdAndOperatorCode(accountId, "HCMC-METRO")).thenReturn(Optional.of(account("admin", PredefinedRole.OPERATOR_ADMIN, true)));
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class, () -> accountService.disableAccount(accountId));

        assertEquals(ErrorCode.OPERATOR_ADMIN_STATUS_CHANGE_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void disableAccount_AlreadyDisabled_ThrowsException() {
        String accountId = UUID.randomUUID().toString();
        when(accountRepository.findByIdAndOperatorCode(accountId, "HCMC-METRO")).thenReturn(Optional.of(account("station01", PredefinedRole.STATION_OPERATOR, false)));
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class, () -> accountService.disableAccount(accountId));

        assertEquals(ErrorCode.ACCOUNT_ALREADY_DISABLED, exception.getErrorCode());
    }

    @Test
    void disableAccount_NullRoles_Success() {
        String accountId = UUID.randomUUID().toString();
        Account account = account("station01", PredefinedRole.STATION_OPERATOR, true);
        account.setRoles(null);
        AccountResponse mapped = AccountResponse.builder().isActive(false).build();

        when(accountRepository.findByIdAndOperatorCode(accountId, "HCMC-METRO")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toAccountResponse(account)).thenReturn(mapped);
        authenticateAs(UUID.randomUUID().toString());

        AccountResponse response = accountService.disableAccount(accountId);

        assertFalse(response.getIsActive());
        verify(accountStatusRedisService).markDisabled(accountId);
    }

    @Test
    void enableAccount_Success() {
        String accountId = UUID.randomUUID().toString();
        Account account = account("station01", PredefinedRole.STATION_OPERATOR, false);
        AccountResponse mapped = AccountResponse.builder().isActive(true).build();

        when(accountRepository.findByIdAndOperatorCode(accountId, "HCMC-METRO")).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);
        when(accountMapper.toAccountResponse(account)).thenReturn(mapped);
        authenticateAs(UUID.randomUUID().toString());

        AccountResponse response = accountService.enableAccount(accountId);

        assertTrue(account.isActive());
        assertTrue(response.getIsActive());
        verify(accountStatusRedisService).markEnabled(accountId);
    }

    @Test
    void enableAccount_AlreadyEnabled_ThrowsException() {
        String accountId = UUID.randomUUID().toString();
        when(accountRepository.findByIdAndOperatorCode(accountId, "HCMC-METRO")).thenReturn(Optional.of(account("station01", PredefinedRole.STATION_OPERATOR, true)));
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class, () -> accountService.enableAccount(accountId));

        assertEquals(ErrorCode.ACCOUNT_ALREADY_ENABLED, exception.getErrorCode());
    }

    @Test
    void previewImportAccounts_DelegatesToImportService() {
        ImportAccountPreviewResponse expected = ImportAccountPreviewResponse.builder().totalRows(1).build();
        when(accountImportService.preview(file)).thenReturn(expected);

        assertSame(expected, accountService.previewImportAccounts(file));
    }

    @Test
    void confirmImportAccounts_DelegatesToImportService() {
        ImportAccountConfirmResponse expected = ImportAccountConfirmResponse.builder().imported(1).build();
        when(accountImportService.confirm(file)).thenReturn(expected);

        assertSame(expected, accountService.confirmImportAccounts(file));
    }

    @Test
    void changePassword_Success_SetsPasswordStatusNormal() {
        String accountId = UUID.randomUUID().toString();
        Account account = account("station01", PredefinedRole.STATION_OPERATOR, true);
        account.setPassword("encoded-current");
        account.setPasswordStatus(PredefinedPasswordStatus.NEED_TO_CHANGE);
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("Temp123456")
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Temp123456", "encoded-current")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("encoded-new");
        authenticateAs(accountId);

        ChangePasswordResponse response = accountService.changePassword(request);

        assertEquals(PredefinedPasswordStatus.NORMAL, response.getPasswordStatus());
        assertEquals(PredefinedPasswordStatus.NORMAL, account.getPasswordStatus());
        assertEquals("encoded-new", account.getPassword());
        verify(accountRepository).save(account);
    }

    @Test
    void changePassword_ConfirmMismatch_ThrowsException() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("Temp123456")
                .newPassword("NewPassword123")
                .confirmPassword("OtherPassword123")
                .build();
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class,
                () -> accountService.changePassword(request));

        assertEquals(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH, exception.getErrorCode());
        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void changePassword_WrongCurrentPassword_ThrowsException() {
        String accountId = UUID.randomUUID().toString();
        Account account = account("station01", PredefinedRole.STATION_OPERATOR, true);
        account.setPassword("encoded-current");
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("Wrong123456")
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Wrong123456", "encoded-current")).thenReturn(false);
        authenticateAs(accountId);

        AppException exception = assertThrows(AppException.class, () -> accountService.changePassword(request));

        assertEquals(ErrorCode.CURRENT_PASSWORD_INCORRECT, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void changePassword_MissingCurrentAccount_ThrowsUnauthenticated() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("Temp123456")
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();

        AppException exception = assertThrows(AppException.class, () -> accountService.changePassword(request));

        assertEquals(ErrorCode.UNAUTHENTICATED, exception.getErrorCode());
        verify(accountRepository, never()).findById(anyString());
    }

    @Test
    void resetAccountPassword_Success_GeneratesTemporaryPassword() {
        String accountId = UUID.randomUUID().toString();
        Account account = account("station01", PredefinedRole.STATION_OPERATOR, true);
        account.setId(accountId);
        account.setPasswordStatus(PredefinedPasswordStatus.NEED_TO_RESET);
        when(accountRepository.findByUsernameAndOperatorCode("station01", "HCMC-METRO")).thenReturn(Optional.of(account));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-temp");
        authenticateAs(UUID.randomUUID().toString());

        ResetAccountPasswordResponse response = accountService.resetAccountPassword(
                ResetAccountPasswordRequest.builder().username("station01").build()
        );

        assertEquals("station01", response.getUsername());
        assertEquals(PredefinedPasswordStatus.NEED_TO_CHANGE, response.getPasswordStatus());
        assertNotNull(response.getTemporaryPassword());
        assertEquals("encoded-temp", account.getPassword());
        assertEquals(PredefinedPasswordStatus.NEED_TO_CHANGE, account.getPasswordStatus());
        verify(accountRepository).save(account);
    }

    @Test
    void resetAccountPassword_NotRequested_ThrowsException() {
        Account account = account("station01", PredefinedRole.STATION_OPERATOR, true);
        account.setPasswordStatus(PredefinedPasswordStatus.NORMAL);
        when(accountRepository.findByUsernameAndOperatorCode("station01", "HCMC-METRO")).thenReturn(Optional.of(account));
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class,
                () -> accountService.resetAccountPassword(ResetAccountPasswordRequest.builder()
                        .username("station01")
                        .build()));

        assertEquals(ErrorCode.PASSWORD_RESET_NOT_REQUESTED, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void resetAccountPassword_DifferentOperator_ThrowsOperatorAccessDenied() {
        Account account = account("station01", PredefinedRole.STATION_OPERATOR, true);
        account.setOperatorCode("HCMC-BUS");
        when(accountRepository.findByUsernameAndOperatorCode("station01", "HCMC-METRO")).thenReturn(Optional.empty());
        when(accountRepository.findByUsername("station01")).thenReturn(Optional.of(account));
        authenticateAs(UUID.randomUUID().toString());

        AppException exception = assertThrows(AppException.class,
                () -> accountService.resetAccountPassword(ResetAccountPasswordRequest.builder()
                        .username("station01")
                        .build()));

        assertEquals(ErrorCode.OPERATOR_ACCESS_DENIED, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    private Account account(String username, String roleName, boolean active) {
        return Account.builder()
                .username(username)
                .operatorCode("HCMC-METRO")
                .isActive(active)
                .roles(Set.of(role(roleName)))
                .build();
    }

    private Role role(String roleName) {
        return Role.builder().name(roleName).build();
    }

    private void authenticateAs(String accountId) {
        CustomUserDetails userDetails = CustomUserDetails.builder()
                .id(accountId)
                .username("station01")
                .operatorCode("HCMC-METRO")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, List.of())
        );
    }
}
