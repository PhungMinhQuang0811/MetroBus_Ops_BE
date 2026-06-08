package com.vdt.auth_ops_service.service;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.constant.PredefinedPasswordStatus;
import com.vdt.auth_ops_service.constant.PredefinedRole;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountConfirmResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountPreviewResponse;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.repository.RoleRepository;
import com.vdt.auth_ops_service.service.Impl.AccountImportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountImportServiceTest {
    @Mock private AccountRepository accountRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountImportService accountImportService;

    @Test
    void preview_ValidRows_ReturnsValidPreview() throws IOException {
        when(accountRepository.existsByUsername("station01")).thenReturn(false);
        when(roleRepository.existsByName(PredefinedRole.STATION_OPERATOR)).thenReturn(true);

        ImportAccountPreviewResponse response = accountImportService.preview(xlsx(row("station01", PredefinedRole.STATION_OPERATOR)));

        assertEquals(1, response.getTotalRows());
        assertEquals(1, response.getValidRows());
        assertEquals(0, response.getInvalidRows());
        assertTrue(response.getErrors().isEmpty());
        assertEquals("station01", response.getItems().getFirst().getUsername());
    }

    @Test
    void preview_DuplicateUsernameAndExistingUsername_ReturnsRowErrors() throws IOException {
        when(accountRepository.existsByUsername("station01")).thenReturn(true);
        when(roleRepository.existsByName(PredefinedRole.STATION_OPERATOR)).thenReturn(true);

        ImportAccountPreviewResponse response = accountImportService.preview(xlsx(
                row("station01", PredefinedRole.STATION_OPERATOR),
                row(" station01 ", PredefinedRole.STATION_OPERATOR)
        ));

        assertEquals(0, response.getValidRows());
        assertEquals(2, response.getInvalidRows());
        assertEquals(4, response.getErrors().size());
    }

    @Test
    void preview_MissingFieldsAndInvalidRole_ReturnsRowErrors() throws IOException {
        when(roleRepository.existsByName(PredefinedRole.STATION_OPERATOR)).thenReturn(true);

        ImportAccountPreviewResponse response = accountImportService.preview(xlsx(
                row("", PredefinedRole.STATION_OPERATOR),
                row("admin01", PredefinedRole.OPERATOR_ADMIN)
        ));

        assertEquals(0, response.getValidRows());
        assertEquals(2, response.getInvalidRows());
        assertEquals(2, response.getErrors().size());
    }

    @Test
    void preview_MissingRoleAndRoleNotInDb_ReturnsRowErrors() throws IOException {
        when(roleRepository.existsByName(PredefinedRole.STATION_OPERATOR)).thenReturn(false);

        ImportAccountPreviewResponse response = accountImportService.preview(xlsx(
                row("station01", ""),
                row("station02", PredefinedRole.STATION_OPERATOR)
        ));

        assertEquals(0, response.getValidRows());
        assertEquals(2, response.getInvalidRows());
        assertEquals(2, response.getErrors().size());
    }

    @Test
    void preview_BlankRows_AreSkipped() throws IOException {
        when(roleRepository.existsByName(PredefinedRole.STATION_OPERATOR)).thenReturn(true);

        ImportAccountPreviewResponse response = accountImportService.preview(xlsxWithBlankRows(
                row("station01", PredefinedRole.STATION_OPERATOR)
        ));

        assertEquals(1, response.getTotalRows());
        assertEquals(1, response.getValidRows());
    }

    @Test
    void preview_OnlyBlankRows_ThrowsException() throws IOException {
        AppException exception = assertThrows(AppException.class,
                () -> accountImportService.preview(xlsxWithBlankRows()));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void preview_EmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "accounts.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[0]
        );

        AppException exception = assertThrows(AppException.class, () -> accountImportService.preview(file));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void preview_InvalidExtension_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "accounts.csv", "text/csv", "x".getBytes());

        AppException exception = assertThrows(AppException.class, () -> accountImportService.preview(file));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void preview_NullFileOrFilename_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                null,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "x".getBytes()
        );

        AppException nullFile = assertThrows(AppException.class, () -> accountImportService.preview(null));
        AppException nullFilename = assertThrows(AppException.class, () -> accountImportService.preview(file));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, nullFile.getErrorCode());
        assertEquals(ErrorCode.IMPORT_FILE_INVALID, nullFilename.getErrorCode());
    }

    @Test
    void preview_CorruptedXlsx_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "accounts.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not an xlsx".getBytes()
        );

        AppException exception = assertThrows(AppException.class, () -> accountImportService.preview(file));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void preview_InvalidHeader_ThrowsException() throws IOException {
        MockMultipartFile file = xlsxWithHeader("user", "role", row("station01", PredefinedRole.STATION_OPERATOR));

        AppException exception = assertThrows(AppException.class, () -> accountImportService.preview(file));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void preview_MissingDataRows_ThrowsException() throws IOException {
        MockMultipartFile file = xlsxWithHeader("username", "roleName");

        AppException exception = assertThrows(AppException.class, () -> accountImportService.preview(file));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void confirm_InvalidRows_ThrowsException() throws IOException {
        AppException exception = assertThrows(AppException.class,
                () -> accountImportService.confirm(xlsx(row("admin01", PredefinedRole.OPERATOR_ADMIN))));

        assertEquals(ErrorCode.IMPORT_FILE_HAS_ERRORS, exception.getErrorCode());
        verify(accountRepository, never()).save(any());
    }

    @Test
    void confirm_RoleMissingAfterPreview_ThrowsException() throws IOException {
        when(roleRepository.existsByName(PredefinedRole.STATION_OPERATOR)).thenReturn(true);
        when(roleRepository.findByName(PredefinedRole.STATION_OPERATOR)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> accountImportService.confirm(xlsx(row("station01", PredefinedRole.STATION_OPERATOR))));

        assertEquals(ErrorCode.INVALID_ROLE_SELECTION, exception.getErrorCode());
    }

    @Test
    void confirm_ValidRows_CreatesAccountsWithTemporaryPassword() throws IOException {
        Role role = Role.builder().name(PredefinedRole.STATION_OPERATOR).build();

        when(accountRepository.existsByUsername("station01")).thenReturn(false);
        when(roleRepository.existsByName(PredefinedRole.STATION_OPERATOR)).thenReturn(true);
        when(roleRepository.findByName(PredefinedRole.STATION_OPERATOR)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account account = invocation.getArgument(0);
            account.setId(UUID.randomUUID().toString());
            return account;
        });

        ImportAccountConfirmResponse response = accountImportService.confirm(xlsx(row("station01", PredefinedRole.STATION_OPERATOR)));

        assertEquals(1, response.getImported());
        assertEquals("station01", response.getItems().getFirst().getUsername());
        assertEquals(9, response.getItems().getFirst().getTemporaryPassword().length());

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account savedAccount = accountCaptor.getValue();
        assertTrue(savedAccount.isActive());
        assertEquals("encoded", savedAccount.getPassword());
        assertEquals(PredefinedPasswordStatus.NEED_TO_CHANGE, savedAccount.getPasswordStatus());
        assertEquals(Set.of(role), savedAccount.getRoles());
    }

    private Object[] row(String username, String roleName) {
        return new Object[]{username, roleName};
    }

    private MockMultipartFile xlsx(Object[]... rows) throws IOException {
        return xlsxWithHeader("username", "roleName", rows);
    }

    private MockMultipartFile xlsxWithHeader(String usernameHeader, String roleHeader, Object[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("accounts");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(usernameHeader);
            header.createCell(1).setCellValue(roleHeader);

            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 1);
                row.createCell(0).setCellValue((String) rows[i][0]);
                row.createCell(1).setCellValue((String) rows[i][1]);
            }

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "accounts.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }

    private MockMultipartFile xlsxWithBlankRows(Object[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("accounts");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("username");
            header.createCell(1).setCellValue("roleName");

            sheet.createRow(1);
            for (int i = 0; i < rows.length; i++) {
                Row row = sheet.createRow(i + 2);
                row.createCell(0).setCellValue((String) rows[i][0]);
                row.createCell(1).setCellValue((String) rows[i][1]);
            }

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "accounts.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }
}
