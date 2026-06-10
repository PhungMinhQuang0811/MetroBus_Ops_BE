package com.vdt.afc_ops_service.common.util;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExcelUtilTest {

    @Test
    void parseRows_ValidWorkbook_SkipsBlankRowsAndNormalizesValues() throws IOException {
        MockMultipartFile file = xlsx(
                new String[]{"routeName", "transportType"},
                new String[]{" Metro Line 1 ", " metro "},
                null,
                new String[]{"Bus Route 01", "BUS"}
        );

        List<ExcelUtil.ExcelRow> rows = ExcelUtil.parseRows(
                file,
                List.of("routeName", "transportType"),
                row -> row,
                ErrorCode.IMPORT_FILE_INVALID
        );

        assertEquals(2, rows.size());
        assertEquals(2, rows.get(0).rowNumber());
        assertEquals("Metro Line 1", rows.get(0).getValue(0));
        assertEquals("metro", rows.get(0).getValue(1));
        assertEquals(4, rows.get(1).rowNumber());
    }

    @Test
    void parseRows_InvalidHeader_ThrowsImportFileInvalid() throws IOException {
        MockMultipartFile file = xlsx(
                new String[]{"name", "type"},
                new String[]{"Metro Line 1", "METRO"}
        );

        AppException exception = assertThrows(AppException.class, () -> ExcelUtil.parseRows(
                file,
                List.of("routeName", "transportType"),
                row -> row,
                ErrorCode.IMPORT_FILE_INVALID
        ));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void parseRows_OnlyBlankRows_ThrowsImportFileInvalid() throws IOException {
        MockMultipartFile file = xlsx(
                new String[]{"routeName", "transportType"},
                null,
                null
        );

        AppException exception = assertThrows(AppException.class, () -> ExcelUtil.parseRows(
                file,
                List.of("routeName", "transportType"),
                row -> row,
                ErrorCode.IMPORT_FILE_INVALID
        ));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void parseRows_NullFile_ThrowsImportFileInvalid() {
        AppException exception = assertThrows(AppException.class, () -> ExcelUtil.parseRows(
                null,
                List.of("routeName", "transportType"),
                row -> row,
                ErrorCode.IMPORT_FILE_INVALID
        ));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void parseRows_InvalidExtension_ThrowsImportFileInvalid() {
        MockMultipartFile file = new MockMultipartFile("file", "routes.csv", "text/csv", "x".getBytes());

        AppException exception = assertThrows(AppException.class, () -> ExcelUtil.parseRows(
                file,
                List.of("routeName", "transportType"),
                row -> row,
                ErrorCode.IMPORT_FILE_INVALID
        ));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void parseRows_NullFilename_ThrowsImportFileInvalid() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                null,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "x".getBytes()
        );

        AppException exception = assertThrows(AppException.class, () -> ExcelUtil.parseRows(
                file,
                List.of("routeName", "transportType"),
                row -> row,
                ErrorCode.IMPORT_FILE_INVALID
        ));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void parseRows_CorruptedXlsx_ThrowsImportFileInvalid() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "routes.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "not an xlsx".getBytes()
        );

        AppException exception = assertThrows(AppException.class, () -> ExcelUtil.parseRows(
                file,
                List.of("routeName", "transportType"),
                row -> row,
                ErrorCode.IMPORT_FILE_INVALID
        ));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void excelRow_GetValueOutOfRange_ReturnsNull() {
        ExcelUtil.ExcelRow row = new ExcelUtil.ExcelRow(2, List.of("Metro Line 1"));

        assertNull(row.getValue(1));
    }

    private MockMultipartFile xlsx(String[] header, String[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("routes");
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < header.length; index++) {
                headerRow.createCell(index).setCellValue(header[index]);
            }

            for (int rowIndex = 0; rowIndex < rows.length; rowIndex++) {
                String[] rowValues = rows[rowIndex];
                if (rowValues == null) {
                    continue;
                }
                Row row = sheet.createRow(rowIndex + 1);
                for (int columnIndex = 0; columnIndex < rowValues.length; columnIndex++) {
                    row.createCell(columnIndex).setCellValue(rowValues[columnIndex]);
                }
            }

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "routes.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }
}
