package com.vdt.afc_ops_service.common.util;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ExcelUtil {

    private ExcelUtil() {
    }

    public static <T> List<T> parseRows(MultipartFile file,
                                        List<String> expectedHeaders,
                                        ExcelRowMapper<T> rowMapper,
                                        ErrorCode invalidFileErrorCode) {
        validateExcelFile(file, invalidFileErrorCode);

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateSheet(sheet, expectedHeaders, invalidFileErrorCode);

            DataFormatter formatter = new DataFormatter();
            List<T> rows = new ArrayList<>();
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (isBlankRow(row, formatter, expectedHeaders.size())) {
                    continue;
                }
                rows.add(rowMapper.map(toExcelRow(rowIndex + 1, row, formatter, expectedHeaders.size())));
            }

            if (rows.isEmpty()) {
                throw new AppException(invalidFileErrorCode);
            }
            return rows;
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof AppException appException) {
                throw appException;
            }
            throw new AppException(invalidFileErrorCode);
        }
    }

    private static void validateExcelFile(MultipartFile file, ErrorCode invalidFileErrorCode) {
        if (file == null || file.isEmpty()) {
            throw new AppException(invalidFileErrorCode);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new AppException(invalidFileErrorCode);
        }
    }

    private static void validateSheet(Sheet sheet, List<String> expectedHeaders,
                                      ErrorCode invalidFileErrorCode) {
        if (sheet == null || sheet.getPhysicalNumberOfRows() < 2) {
            throw new AppException(invalidFileErrorCode);
        }

        DataFormatter formatter = new DataFormatter();
        Row header = sheet.getRow(0);
        if (header == null) {
            throw new AppException(invalidFileErrorCode);
        }

        for (int index = 0; index < expectedHeaders.size(); index++) {
            if (!expectedHeaders.get(index).equals(getCellValue(header, index, formatter))) {
                throw new AppException(invalidFileErrorCode);
            }
        }
    }

    private static boolean isBlankRow(Row row, DataFormatter formatter, int columnCount) {
        if (row == null) {
            return true;
        }
        for (int index = 0; index < columnCount; index++) {
            if (getCellValue(row, index, formatter) != null) {
                return false;
            }
        }
        return true;
    }

    private static ExcelRow toExcelRow(int rowNumber, Row row, DataFormatter formatter, int columnCount) {
        List<String> values = new ArrayList<>(columnCount);
        for (int index = 0; index < columnCount; index++) {
            values.add(getCellValue(row, index, formatter));
        }
        return new ExcelRow(rowNumber, values);
    }

    private static String getCellValue(Row row, int cellIndex, DataFormatter formatter) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(cellIndex);
        return SearchFilterUtil.normalize(formatter.formatCellValue(cell));
    }

    @FunctionalInterface
    public interface ExcelRowMapper<T> {
        T map(ExcelRow row);
    }

    public record ExcelRow(int rowNumber, List<String> values) {
        public String getValue(int index) {
            return index < values.size() ? values.get(index) : null;
        }
    }
}
