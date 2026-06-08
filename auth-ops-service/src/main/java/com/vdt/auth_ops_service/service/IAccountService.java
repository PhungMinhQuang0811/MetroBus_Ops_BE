package com.vdt.auth_ops_service.service;

import com.vdt.auth_ops_service.dto.request.account.CreateAccountRequest;
import com.vdt.auth_ops_service.dto.response.PageResponse;
import com.vdt.auth_ops_service.dto.response.account.AccountResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountConfirmResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountPreviewResponse;
import org.springframework.web.multipart.MultipartFile;

public interface IAccountService {
    PageResponse<AccountResponse> listAccounts(String keyword, String role, Boolean isActive, int page, int size);

    AccountResponse getAccount(String accountId);

    AccountResponse createAccount(CreateAccountRequest request);

    AccountResponse disableAccount(String accountId);

    AccountResponse enableAccount(String accountId);

    ImportAccountPreviewResponse previewImportAccounts(MultipartFile file);

    ImportAccountConfirmResponse confirmImportAccounts(MultipartFile file);
}
