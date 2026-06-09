package com.vdt.auth_ops_service.service;

import com.vdt.auth_ops_service.dto.request.account.ChangePasswordRequest;
import com.vdt.auth_ops_service.dto.request.account.CreateAccountRequest;
import com.vdt.auth_ops_service.dto.request.account.ResetAccountPasswordRequest;
import com.vdt.auth_ops_service.dto.response.PageResponse;
import com.vdt.auth_ops_service.dto.response.account.AccountResponse;
import com.vdt.auth_ops_service.dto.response.account.ChangePasswordResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountConfirmResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountPreviewResponse;
import com.vdt.auth_ops_service.dto.response.account.ResetAccountPasswordResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IAccountService {
    PageResponse<AccountResponse> listAccounts(String keyword, String role, Boolean isActive, String passwordStatus, int page, int size);

    AccountResponse getAccount(String accountId);

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse disableAccount(String accountId);

    AccountResponse enableAccount(String accountId);

    ImportAccountPreviewResponse previewImportAccounts(MultipartFile file);

    ImportAccountConfirmResponse confirmImportAccounts(MultipartFile file);

    ChangePasswordResponse changePassword(ChangePasswordRequest request);

    ResetAccountPasswordResponse resetAccountPassword(ResetAccountPasswordRequest request);
}
