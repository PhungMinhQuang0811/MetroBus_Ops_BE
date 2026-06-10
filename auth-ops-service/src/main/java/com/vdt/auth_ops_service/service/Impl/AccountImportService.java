package com.vdt.auth_ops_service.service.Impl;

import com.vdt.auth_ops_service.common.exception.AppException;
import com.vdt.auth_ops_service.common.exception.ErrorCode;
import com.vdt.auth_ops_service.common.util.ExcelUtil;
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
import com.vdt.auth_ops_service.security.util.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
    static final List<String> IMPORT_HEADERS = List.of(USERNAME_HEADER, ROLE_HEADER);
    static final Set<String> IMPORT_ALLOWED_ROLES = Set.of(
            PredefinedRole.OPERATOR_MANAGER,
            PredefinedRole.STATION_OPERATOR
    );

    AccountRepository accountRepository;
    RoleRepository roleRepository;
    PasswordEncoder passwordEncoder;

    public ImportAccountPreviewResponse preview(MultipartFile file) {
        return buildImportPreview(parseImportRows(file), SecurityUtils.getRequiredCurrentOperatorCode());
    }

    public ImportAccountConfirmResponse confirm(MultipartFile file) {
        String operatorCode = SecurityUtils.getRequiredCurrentOperatorCode();
        ImportAccountPreviewResponse preview = buildImportPreview(parseImportRows(file), operatorCode);
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
                    .operatorCode(operatorCode)
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
                    .operatorCode(savedAccount.getOperatorCode())
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
        return ExcelUtil.parseRows(
                file,
                IMPORT_HEADERS,
                row -> new ImportAccountRow(
                        row.rowNumber(),
                        row.getValue(USERNAME_COLUMN_INDEX),
                        row.getValue(ROLE_COLUMN_INDEX)
                ),
                ErrorCode.IMPORT_FILE_INVALID
        );
    }

    private ImportAccountPreviewResponse buildImportPreview(List<ImportAccountRow> rows, String operatorCode) {
        Set<String> usernamesInFile = new HashSet<>();
        Set<String> duplicateUsernames = rows.stream()
                .map(row -> normalize(row.username()))
                .filter(username -> username != null)
                .filter(username -> !usernamesInFile.add(username.toLowerCase()))
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<ImportAccountPreviewItem> items = rows.stream()
                .map(row -> validateImportRow(row, duplicateUsernames, operatorCode))
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

    private ImportAccountPreviewItem validateImportRow(ImportAccountRow row, Set<String> duplicateUsernames, String operatorCode) {
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
                .operatorCode(operatorCode)
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

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ImportAccountRow(Integer rowNumber, String username, String roleName) {
    }
}
