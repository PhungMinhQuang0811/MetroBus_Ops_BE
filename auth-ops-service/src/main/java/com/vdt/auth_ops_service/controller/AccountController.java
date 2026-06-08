package com.vdt.auth_ops_service.controller;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.dto.request.account.CreateAccountRequest;
import com.vdt.auth_ops_service.dto.response.ApiResponse;
import com.vdt.auth_ops_service.dto.response.PageResponse;
import com.vdt.auth_ops_service.dto.response.account.AccountResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountConfirmResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountPreviewResponse;
import com.vdt.auth_ops_service.service.IAccountService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountController {
    IAccountService accountService;

    @GetMapping("/list-accounts")
    public ApiResponse<PageResponse<AccountResponse>> listAccounts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.<PageResponse<AccountResponse>>builder()
                .result(accountService.listAccounts(keyword, role, isActive, page, size))
                .build();
    }

    @GetMapping("/get-account/{accountId}")
    public ApiResponse<AccountResponse> getAccount(@PathVariable String accountId) {
        return ApiResponse.<AccountResponse>builder()
                .result(accountService.getAccount(accountId))
                .build();
    }

    @PostMapping("/create-account")
    public ApiResponse<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ApiResponse.<AccountResponse>builder()
                .result(accountService.createAccount(request))
                .build();
    }

    @PostMapping("/disable-account/{accountId}")
    public ApiResponse<AccountResponse> disableAccount(@PathVariable String accountId) {
        return ApiResponse.<AccountResponse>builder()
                .result(accountService.disableAccount(accountId))
                .build();
    }

    @PostMapping("/enable-account/{accountId}")
    public ApiResponse<AccountResponse> enableAccount(@PathVariable String accountId) {
        return ApiResponse.<AccountResponse>builder()
                .result(accountService.enableAccount(accountId))
                .build();
    }

    @PostMapping("/preview-import-accounts")
    public ApiResponse<ImportAccountPreviewResponse> previewImportAccounts(
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.<ImportAccountPreviewResponse>builder()
                .result(accountService.previewImportAccounts(resolveSingleImportFile(files)))
                .build();
    }

    @PostMapping("/confirm-import-accounts")
    public ApiResponse<ImportAccountConfirmResponse> confirmImportAccounts(
            @RequestPart(value = "file", required = false) List<MultipartFile> files
    ) {
        return ApiResponse.<ImportAccountConfirmResponse>builder()
                .result(accountService.confirmImportAccounts(resolveSingleImportFile(files)))
                .build();
    }

    private MultipartFile resolveSingleImportFile(List<MultipartFile> files) {
        if (files == null || files.size() != 1) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }

        return files.getFirst();
    }
}
