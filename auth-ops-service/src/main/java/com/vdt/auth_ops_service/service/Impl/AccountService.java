package com.vdt.auth_ops_service.service.Impl;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.common.util.PasswordUtil;
import com.vdt.auth_ops_service.constant.PredefinedPasswordStatus;
import com.vdt.auth_ops_service.constant.PredefinedRole;
import com.vdt.auth_ops_service.dto.request.account.CreateAccountRequest;
import com.vdt.auth_ops_service.dto.response.PageResponse;
import com.vdt.auth_ops_service.dto.response.account.AccountResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountConfirmResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountPreviewResponse;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.mapper.AccountMapper;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.repository.RoleRepository;
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

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AccountResponse> listAccounts(String keyword, String role, Boolean isActive, int page, int size) {
        String normalizedKeyword = normalize(keyword);
        String normalizedRole = normalize(role);

        validateListAccountParams(normalizedKeyword, normalizedRole, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<Account> accounts = accountRepository.searchAccounts(
                toKeywordPattern(normalizedKeyword),
                normalizedRole,
                isActive,
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
        return accountMapper.toAccountResponse(getAccountEntity(accountId));
    }

    @Override
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        Set<Role> roles = resolveRoles(request.getRoleNames());
        String temporaryPassword = PasswordUtil.generateTemporaryPassword();
        Account account = Account.builder()
                .username(request.getUsername())
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
        Account account = getAccountEntity(accountId);
        validateOperatorAdminStatusNotChanged(account);
        if (!account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_DISABLED);
        }

        account.setActive(false);
        return accountMapper.toAccountResponse(accountRepository.save(account));
    }

    @Override
    @Transactional
    public AccountResponse enableAccount(String accountId) {
        validateAccountId(accountId);
        Account account = getAccountEntity(accountId);
        validateOperatorAdminStatusNotChanged(account);
        if (account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_ENABLED);
        }

        account.setActive(true);
        return accountMapper.toAccountResponse(accountRepository.save(account));
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

    private Account getAccountEntity(String accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateListAccountParams(String keyword, String role, int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGE_REQUEST);
        }

        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            throw new AppException(ErrorCode.INVALID_SEARCH_KEYWORD);
        }

        if (role != null && !roleRepository.existsByName(role)) {
            throw new AppException(ErrorCode.INVALID_ROLE_SELECTION);
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

    private Set<Role> resolveRoles(Set<String> roleNames) {
        Set<Role> roles = roleRepository.findAllByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            throw new AppException(ErrorCode.INVALID_ROLE_SELECTION);
        }
        return roles;
    }

    private void validateOperatorAdminStatusNotChanged(Account account) {
        if (hasRole(account.getRoles(), PredefinedRole.OPERATOR_ADMIN)) {
            throw new AppException(ErrorCode.OPERATOR_ADMIN_STATUS_CHANGE_NOT_ALLOWED);
        }
    }

    private boolean hasRole(Set<Role> roles, String roleName) {
        return roles != null && roles.stream().anyMatch(role -> roleName.equals(role.getName()));
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String toKeywordPattern(String keyword) {
        return keyword == null ? "%" : "%" + keyword.toLowerCase() + "%";
    }
}
