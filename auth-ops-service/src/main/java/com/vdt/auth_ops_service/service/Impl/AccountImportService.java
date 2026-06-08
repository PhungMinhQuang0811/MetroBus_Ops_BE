package com.vdt.auth_ops_service.service.Impl;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.common.util.PasswordUtil;
import com.vdt.auth_ops_service.constant.PredefinedPasswordStatus;
import com.vdt.auth_ops_service.constant.PredefinedRole;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountConfirmResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountItemResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountPreviewItem;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountPreviewResponse;
import com.vdt.auth_ops_service.dto.response.account.ImportAccountRowError;
import com.vdt.auth_ops_service.entity.Account;
import com.vdt.auth_ops_service.entity.Role;
import com.vdt.auth_ops_service.repository.AccountRepository;
import com.vdt.auth_ops_service.repository.RoleRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AccountImportService {
    static final int USERNAME_COLUMN_INDEX = 0;
    static final int ROLE_COLUMN_INDEX = 1;
    static final String USERNAME_HEADER = "username";
    static final String ROLE_HEADER = "roleName";
    static final Set<String> IMPORT_ALLOWED_ROLES = Set.of(
            PredefinedRole.OPERATOR_MANAGER,
            PredefinedRole.STATION_OPERATOR
    );

    AccountRepository accountRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

    public ImportAccountPreviewResponse preview(MultipartFile file) {
        return buildImportPreview(parseImportRows(file));
    }

    public ImportAccountConfirmResponse confirm(MultipartFile file) {
        ImportAccountPreviewResponse preview = buildImportPreview(parseImportRows(file));
        if (preview.getInvalidRows() > 0) {
            throw new AppException(ErrorCode.IMPORT_FILE_HAS_ERRORS);
        }

        List<ImportAccountItemResponse> importedItems = new ArrayList<>();
        for (ImportAccountPreviewItem item : preview.getItems()) {
            Role role = roleRepository.findByName(item.getRoleName())
                    .orElseThrow(() -> new AppException(ErrorCode.INVALID_ROLE_SELECTION));
            String temporaryPassword = PasswordUtil.generateTemporaryPassword();
            Account account = Account.builder()
                    .username(item.getUsername())
                    .password(passwordEncoder.encode(temporaryPassword))
                    .isActive(true)
                    .passwordStatus(PredefinedPasswordStatus.NEED_TO_CHANGE)
                    .roles(Set.of(role))
                    .build();

            Account savedAccount = accountRepository.save(account);
            importedItems.add(ImportAccountItemResponse.builder()
                    .row(item.getRow())
                    .id(savedAccount.getId())
                    .username(savedAccount.getUsername())
                    .roles(savedAccount.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                    .isActive(savedAccount.isActive())
                    .passwordStatus(savedAccount.getPasswordStatus())
                    .temporaryPassword(temporaryPassword)
                    .build());
        }

        return ImportAccountConfirmResponse.builder()
                .imported(importedItems.size())
                .items(importedItems)
                .build();
    }

    private List<ImportAccountRow> parseImportRows(MultipartFile file) {
        validateImportFile(file);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateImportSheet(sheet);

            List<ImportAccountRow> rows = new ArrayList<>();
            DataFormatter formatter = new DataFormatter();
            int lastRowNumber = sheet.getLastRowNum();
            for (int rowIndex = 1; rowIndex <= lastRowNumber; rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter)) {
                    continue;
                }

                rows.add(new ImportAccountRow(
                        rowIndex + 1,
                        getCellValue(row, USERNAME_COLUMN_INDEX, formatter),
                        getCellValue(row, ROLE_COLUMN_INDEX, formatter)
                ));
            }

            if (rows.isEmpty()) {
                throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
            }

            return rows;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }
    }

    private ImportAccountPreviewResponse buildImportPreview(List<ImportAccountRow> rows) {
        Set<String> usernamesInFile = new HashSet<>();
        Set<String> duplicateUsernames = rows.stream()
                .map(row -> normalize(row.username()))
                .filter(username -> username != null)
                .filter(username -> !usernamesInFile.add(username.toLowerCase()))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<ImportAccountPreviewItem> items = rows.stream()
                .map(row -> validateImportRow(row, duplicateUsernames))
                .toList();

        List<ImportAccountRowError> errors = items.stream()
                .flatMap(item -> item.getErrors().stream())
                .toList();

        return ImportAccountPreviewResponse.builder()
                .totalRows(rows.size())
                .validRows((int) items.stream().filter(ImportAccountPreviewItem::getValid).count())
                .invalidRows((int) items.stream().filter(item -> !item.getValid()).count())
                .items(items)
                .errors(errors)
                .build();
    }

    private ImportAccountPreviewItem validateImportRow(ImportAccountRow row, Set<String> duplicateUsernames) {
        String username = normalize(row.username());
        String roleName = normalize(row.roleName());
        List<ImportAccountRowError> errors = new ArrayList<>();

        if (username == null) {
            errors.add(importError(row.rowNumber(), USERNAME_HEADER, "Username is required"));
        } else {
            if (duplicateUsernames.contains(username.toLowerCase())) {
                errors.add(importError(row.rowNumber(), USERNAME_HEADER, "Username is duplicated in import file"));
            }
            if (accountRepository.existsByUsername(username)) {
                errors.add(importError(row.rowNumber(), USERNAME_HEADER, "Username already exists"));
            }
        }

        if (roleName == null) {
            errors.add(importError(row.rowNumber(), ROLE_HEADER, "Role is required"));
        } else if (!IMPORT_ALLOWED_ROLES.contains(roleName) || !roleRepository.existsByName(roleName)) {
            errors.add(importError(row.rowNumber(), ROLE_HEADER, "Invalid operator role selection"));
        }

        return ImportAccountPreviewItem.builder()
                .row(row.rowNumber())
                .username(username)
                .roleName(roleName)
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private ImportAccountRowError importError(Integer row, String field, String message) {
        return ImportAccountRowError.builder()
                .row(row)
                .field(field)
                .message(message)
                .build();
    }

    private void validateImportFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }
    }

    private void validateImportSheet(Sheet sheet) {
        if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }

        DataFormatter formatter = new DataFormatter();
        Row header = sheet.getRow(0);
        if (header == null
                || !USERNAME_HEADER.equals(getCellValue(header, USERNAME_COLUMN_INDEX, formatter))
                || !ROLE_HEADER.equals(getCellValue(header, ROLE_COLUMN_INDEX, formatter))) {
            throw new AppException(ErrorCode.IMPORT_FILE_INVALID);
        }
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        if (row == null) {
            return true;
        }

        return getCellValue(row, USERNAME_COLUMN_INDEX, formatter) == null
                && getCellValue(row, ROLE_COLUMN_INDEX, formatter) == null;
    }

    private String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null) {
            return null;
        }

        Cell cell = row.getCell(cellIndex);
        String value = formatter.formatCellValue(cell);
        return normalize(value);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ImportAccountRow(Integer rowNumber, String username, String roleName) {
    }
}
