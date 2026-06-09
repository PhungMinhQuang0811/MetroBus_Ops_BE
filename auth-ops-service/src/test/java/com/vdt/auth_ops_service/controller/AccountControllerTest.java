package com.vdt.auth_ops_service.controller;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.dto.request.account.ChangePasswordRequest;
import com.vdt.auth_ops_service.dto.request.account.CreateAccountRequest;
import com.vdt.auth_ops_service.dto.request.account.ResetAccountPasswordRequest;
import com.vdt.auth_ops_service.dto.response.PageResponse;
import com.vdt.auth_ops_service.dto.response.account.AccountResponse;
import com.vdt.auth_ops_service.dto.response.account.ChangePasswordResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountConfirmResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountPreviewResponse;
import com.vdt.auth_ops_service.dto.response.account.ResetAccountPasswordResponse;
import com.vdt.auth_ops_service.service.IAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {
    @Mock
    private IAccountService accountService;

    private AccountController controller;

    @BeforeEach
    void setUp() {
        controller = new AccountController(accountService);
    }

    @Test
    void listAccounts_DelegatesToService() {
        PageResponse<AccountResponse> expected = PageResponse.<AccountResponse>builder()
                .items(List.of(AccountResponse.builder().username("station01").build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build();
        when(accountService.listAccounts("station", "STATION_OPERATOR", true, "NEED_TO_RESET", 0, 20))
                .thenReturn(expected);

        assertSame(expected, controller.listAccounts("station", "STATION_OPERATOR", true, "NEED_TO_RESET", 0, 20).getResult());
    }

    @Test
    void getAccount_DelegatesToService() {
        AccountResponse expected = AccountResponse.builder()
                .id("account-id")
                .username("station01")
                .build();
        when(accountService.getAccount("account-id")).thenReturn(expected);

        assertSame(expected, controller.getAccount("account-id").getResult());
    }

    @Test
    void createAccount_DelegatesToService() {
        CreateAccountRequest request = CreateAccountRequest.builder()
                .username("station01")
                .roleNames(Set.of("STATION_OPERATOR"))
                .build();
        AccountResponse expected = AccountResponse.builder()
                .username("station01")
                .build();
        when(accountService.createAccount(request)).thenReturn(expected);

        assertSame(expected, controller.createAccount(request).getResult());
    }

    @Test
    void disableAccount_DelegatesToService() {
        AccountResponse expected = AccountResponse.builder()
                .id("account-id")
                .isActive(false)
                .build();
        when(accountService.disableAccount("account-id")).thenReturn(expected);

        assertSame(expected, controller.disableAccount("account-id").getResult());
    }

    @Test
    void enableAccount_DelegatesToService() {
        AccountResponse expected = AccountResponse.builder()
                .id("account-id")
                .isActive(true)
                .build();
        when(accountService.enableAccount("account-id")).thenReturn(expected);

        assertSame(expected, controller.enableAccount("account-id").getResult());
    }

    @Test
    void previewImportAccounts_SingleFile_DelegatesToService() {
        MockMultipartFile file = importFile("accounts.xlsx");
        ImportAccountPreviewResponse expected = ImportAccountPreviewResponse.builder()
                .totalRows(1)
                .build();
        when(accountService.previewImportAccounts(file)).thenReturn(expected);

        assertSame(expected, controller.previewImportAccounts(List.of(file)).getResult());
    }

    @Test
    void previewImportAccounts_NullFiles_ThrowsImportFileInvalid() {
        AppException exception = assertThrows(
                AppException.class,
                () -> controller.previewImportAccounts(null)
        );

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
        verifyNoInteractions(accountService);
    }

    @Test
    void previewImportAccounts_MultipleFiles_ThrowsImportFileInvalid() {
        MockMultipartFile firstFile = importFile("first.xlsx");
        MockMultipartFile secondFile = importFile("second.xlsx");

        AppException exception = assertThrows(
                AppException.class,
                () -> controller.previewImportAccounts(List.of(firstFile, secondFile))
        );

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
        verify(accountService, never()).previewImportAccounts(firstFile);
        verify(accountService, never()).previewImportAccounts(secondFile);
    }

    @Test
    void confirmImportAccounts_SingleFile_DelegatesToService() {
        MockMultipartFile file = importFile("accounts.xlsx");
        ImportAccountConfirmResponse expected = ImportAccountConfirmResponse.builder()
                .imported(1)
                .build();
        when(accountService.confirmImportAccounts(file)).thenReturn(expected);

        assertSame(expected, controller.confirmImportAccounts(List.of(file)).getResult());
    }

    @Test
    void confirmImportAccounts_EmptyFiles_ThrowsImportFileInvalid() {
        AppException exception = assertThrows(
                AppException.class,
                () -> controller.confirmImportAccounts(List.of())
        );

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
        verifyNoInteractions(accountService);
    }

    @Test
    void changePassword_DelegatesToService() {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("Temp123456")
                .newPassword("NewPassword123")
                .confirmPassword("NewPassword123")
                .build();
        ChangePasswordResponse expected = ChangePasswordResponse.builder()
                .passwordStatus("NORMAL")
                .build();
        when(accountService.changePassword(request)).thenReturn(expected);

        assertSame(expected, controller.changePassword(request).getResult());
    }

    @Test
    void resetAccountPassword_DelegatesToService() {
        ResetAccountPasswordRequest request = ResetAccountPasswordRequest.builder()
                .username("station01")
                .build();
        ResetAccountPasswordResponse expected = ResetAccountPasswordResponse.builder()
                .username("station01")
                .passwordStatus("NEED_TO_CHANGE")
                .temporaryPassword("A7xQp2Lm9")
                .build();
        when(accountService.resetAccountPassword(request)).thenReturn(expected);

        assertSame(expected, controller.resetAccountPassword(request).getResult());
    }

    private MockMultipartFile importFile(String filename) {
        return new MockMultipartFile(
                "file",
                filename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1}
        );
    }
}
