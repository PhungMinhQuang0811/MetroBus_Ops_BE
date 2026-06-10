package com.vdt.auth_ops_service.service.Impl;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.common.util.PasswordUtil;
import com.vdt.auth_ops_service.common.util.SearchFilterUtil;
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
import com.vdt.auth_ops_service.security.service.AccountStatusRedisService;
import com.vdt.auth_ops_service.security.util.SecurityUtils;
import com.vdt.auth_ops_service.service.IAccountService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountService implements IAccountService {
    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_KEYWORD_LENGTH = 50;

    AccountRepository accountRepository;
    RoleRepository roleRepository;
    AccountMapper accountMapper;
    PasswordEncoder passwordEncoder;
    AccountImportService accountImportService;
    AccountStatusRedisService accountStatusRedisService;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> listAccounts(String keyword, String role, Boolean isActive, String passwordStatus, int page, int size) {
        String normalizedKeyword = SearchFilterUtil.normalize(keyword);
        String normalizedRole = SearchFilterUtil.normalize(role);
        String normalizedPasswordStatus = SearchFilterUtil.normalize(passwordStatus);

        validateListAccountParams(normalizedKeyword, normalizedRole, normalizedPasswordStatus, page, size);
        String operatorCode = SecurityUtils.getRequiredCurrentOperatorCode();

        Pageable pageable = PageRequest.of(page, size);
        Page<Account> accounts = accountRepository.searchAccounts(
                operatorCode,
                SearchFilterUtil.toKeywordPattern(normalizedKeyword),
                normalizedRole,
                isActive,
                normalizedPasswordStatus,
                pageable
        );

        return PageResponse.<AccountResponse>builder()
                .items(accounts.getContent().stream()
                        .map(accountMapper::toAccountResponse)
                        .toList())
                .page(accounts.getNumber())
                .size(accounts.getSize())
                .totalElements(accounts.getTotalElements())
                .totalPages(accounts.getTotalPages())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getAccount(String accountId) {
        validateAccountId(accountId);
        return accountMapper.toAccountResponse(getAccountEntityInCurrentOperator(accountId));
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        String operatorCode = SecurityUtils.getRequiredCurrentOperatorCode();
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        Set<Role> roles = roleRepository.findAllByNameIn(request.getRoleNames());
        if (roles.size() != request.getRoleNames().size()) {
            throw new AppException(ErrorCode.INVALID_ROLE_SELECTION);
        }

        String temporaryPassword = PasswordUtil.generateTemporaryPassword();
        Account account = Account.builder()
                .username(request.getUsername())
                .operatorCode(operatorCode)
                .password(passwordEncoder.encode(temporaryPassword))
                .isActive(true)
                .passwordStatus(PredefinedPasswordStatus.NEED_TO_CHANGE)
                .roles(roles)
                .build();

        AccountResponse response = accountMapper.toAccountResponse(accountRepository.save(account));
        response.setTemporaryPassword(temporaryPassword);
        return response;
    }

    @Override
    @Transactional
    public AccountResponse disableAccount(String accountId) {
        validateAccountId(accountId);
        Account account = getAccountEntityInCurrentOperator(accountId);
        validateOperatorAdminStatusNotChanged(account);
        if (!account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_DISABLED);
        }

        account.setActive(false);
        Account savedAccount = accountRepository.save(account);
        accountStatusRedisService.markDisabled(accountId);
        return accountMapper.toAccountResponse(savedAccount);
    }

    @Override
    @Transactional
    public AccountResponse enableAccount(String accountId) {
        validateAccountId(accountId);
        Account account = getAccountEntityInCurrentOperator(accountId);
        validateOperatorAdminStatusNotChanged(account);
        if (account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_ENABLED);
        }

        account.setActive(true);
        Account savedAccount = accountRepository.save(account);
        accountStatusRedisService.markEnabled(accountId);
        return accountMapper.toAccountResponse(savedAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public ImportAccountPreviewResponse previewImportAccounts(MultipartFile file) {
        return accountImportService.preview(file);
    }

    @Override
    @Transactional
    public ImportAccountConfirmResponse confirmImportAccounts(MultipartFile file) {
        return accountImportService.confirm(file);
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
        String accountId = SecurityUtils.getCurrentAccountId();
        if (accountId == null || accountId.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }

        Account account = getAccountEntity(accountId);
        if (!account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_DISABLED);
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new AppException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
        }

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        account.setPasswordStatus(PredefinedPasswordStatus.NORMAL);
        accountRepository.save(account);

        return ChangePasswordResponse.builder()
                .passwordStatus(account.getPasswordStatus())
                .build();
    }

    @Override
    @Transactional
    public ResetAccountPasswordResponse resetAccountPassword(ResetAccountPasswordRequest request) {
        String operatorCode = SecurityUtils.getRequiredCurrentOperatorCode();
        Account account = accountRepository.findByUsernameAndOperatorCode(request.getUsername(), operatorCode)
                .orElseGet(() -> {
                    if (accountRepository.findByUsername(request.getUsername()).isPresent()) {
                        throw new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
                    }
                    throw new AppException(ErrorCode.USER_NOT_FOUND);
                });
        if (!PredefinedPasswordStatus.NEED_TO_RESET.equals(account.getPasswordStatus())) {
            throw new AppException(ErrorCode.PASSWORD_RESET_NOT_REQUESTED);
        }

        String temporaryPassword = PasswordUtil.generateTemporaryPassword();

        account.setPassword(passwordEncoder.encode(temporaryPassword));
        account.setPasswordStatus(PredefinedPasswordStatus.NEED_TO_CHANGE);
        accountRepository.save(account);

        return ResetAccountPasswordResponse.builder()
                .username(account.getUsername())
                .passwordStatus(account.getPasswordStatus())
                .temporaryPassword(temporaryPassword)
                .build();
    }

    private Account getAccountEntity(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private Account getAccountEntityInCurrentOperator(String accountId) {
        String operatorCode = SecurityUtils.getRequiredCurrentOperatorCode();
        return accountRepository.findByIdAndOperatorCode(accountId, operatorCode)
                .orElseGet(() -> {
                    if (accountRepository.existsById(accountId)) {
                        throw new AppException(ErrorCode.OPERATOR_ACCESS_DENIED);
                    }
                    throw new AppException(ErrorCode.USER_NOT_FOUND);
                });
    }

    private void validateListAccountParams(String keyword, String role, String passwordStatus, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }

        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new AppException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }

        if (role != null && !roleRepository.existsByName(role)) {
            throw new AppException(ErrorCode.INVALID_ROLE_SELECTION);
        }

        if (passwordStatus != null && !Set.of(
                PredefinedPasswordStatus.NORMAL,
                PredefinedPasswordStatus.NEED_TO_CHANGE,
                PredefinedPasswordStatus.NEED_TO_RESET
        ).contains(passwordStatus)) {
            throw new AppException(ErrorCode.INVALID_PASSWORD_STATUS);
        }
    }

    private void validateAccountId(String accountId) {
        if (accountId == null || accountId.isBlank()) {
            throw new AppException(ErrorCode.INVALID_ACCOUNT_ID);
        }

        try {
            UUID.fromString(accountId);
        } catch (IllegalArgumentException exception) {
            throw new AppException(ErrorCode.INVALID_ACCOUNT_ID);
        }
    }

    private void validateOperatorAdminStatusNotChanged(Account account) {
        if (account.getRoles() != null && account.getRoles().stream()
                .anyMatch(role -> PredefinedRole.OPERATOR_ADMIN.equals(role.getName()))) {
            throw new AppException(ErrorCode.OPERATOR_ADMIN_STATUS_CHANGE_NOT_ALLOWED);
        }
    }
}
