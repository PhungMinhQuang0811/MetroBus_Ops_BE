package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.ExcelUtil;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedDeviceDirection;
import com.vdt.afc_ops_service.constant.PredefinedDeviceStatus;
import com.vdt.afc_ops_service.constant.PredefinedDeviceType;
import com.vdt.afc_ops_service.dto.response.device.ImportDeviceConfirmResponse;
import com.vdt.afc_ops_service.dto.response.device.ImportDeviceItemResponse;
import com.vdt.afc_ops_service.dto.response.device.ImportDevicePreviewItem;
import com.vdt.afc_ops_service.dto.response.device.ImportDevicePreviewResponse;
import com.vdt.afc_ops_service.dto.response.device.ImportDeviceRowError;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.generator.DeviceCodeGenerator;
import com.vdt.afc_ops_service.service.generator.DeviceSecretGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeviceImportService {

    static final int STATION_CODE_COLUMN_INDEX = 0;
    static final int DEVICE_TYPE_COLUMN_INDEX = 1;
    static final int DIRECTION_COLUMN_INDEX = 2;
    static final int FIRMWARE_VERSION_COLUMN_INDEX = 3;
    static final int MAX_FIRMWARE_VERSION_LENGTH = 100;
    static final String STATION_CODE_HEADER = "stationCode";
    static final String DEVICE_TYPE_HEADER = "deviceType";
    static final String DIRECTION_HEADER = "direction";
    static final String FIRMWARE_VERSION_HEADER = "firmwareVersion";
    static final List<String> IMPORT_HEADERS = List.of(
            STATION_CODE_HEADER,
            DEVICE_TYPE_HEADER,
            DIRECTION_HEADER,
            FIRMWARE_VERSION_HEADER
    );

    StationRepository stationRepository;
    DeviceRepository deviceRepository;
    DeviceCodeGenerator deviceCodeGenerator;
    DeviceSecretGenerator deviceSecretGenerator;
    SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public ImportDevicePreviewResponse preview(MultipartFile file) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        return buildImportPreview(operator, parseImportRows(file));
    }

    @Transactional
    public ImportDeviceConfirmResponse confirm(MultipartFile file) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        ImportDevicePreviewResponse preview = buildImportPreview(operator, parseImportRows(file));
        if (preview.getInvalidRows() > 0) {
            throw new AppException(ErrorCode.IMPORT_FILE_HAS_ERRORS);
        }

        String accountId = SecurityUtils.getRequiredCurrentAccountId();
        List<ImportDeviceItemResponse> importedItems = new ArrayList<>();
        for (ImportDevicePreviewItem item : preview.getItems()) {
            Station station = stationRepository.findByIdAndRouteOperatorId(item.getStationId(), operator.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.STATION_NOT_FOUND));
            String deviceSecret = deviceSecretGenerator.generate();
            Device device = Device.builder()
                    .station(station)
                    .deviceCode(deviceCodeGenerator.generate(station))
                    .deviceType(item.getDeviceType())
                    .direction(item.getDirection())
                    .status(item.getStatus())
                    .firmwareVersion(item.getFirmwareVersion())
                    .deviceSecret(deviceSecret)
                    .createdByAccountId(accountId)
                    .build();

            Device savedDevice = deviceRepository.save(device);
            importedItems.add(ImportDeviceItemResponse.builder()
                    .row(item.getRow())
                    .id(savedDevice.getId())
                    .stationId(station.getId())
                    .stationCode(station.getStationCode())
                    .stationName(station.getStationName())
                    .deviceCode(savedDevice.getDeviceCode())
                    .deviceType(savedDevice.getDeviceType())
                    .direction(savedDevice.getDirection())
                    .status(savedDevice.getStatus())
                    .firmwareVersion(savedDevice.getFirmwareVersion())
                    .deviceSecret(deviceSecret)
                    .build());
        }

        return ImportDeviceConfirmResponse.builder()
                .imported(importedItems.size())
                .items(importedItems)
                .build();
    }

    private List<ImportDeviceRow> parseImportRows(MultipartFile file) {
        return ExcelUtil.parseRows(
                file,
                IMPORT_HEADERS,
                row -> new ImportDeviceRow(
                        row.rowNumber(),
                        row.getValue(STATION_CODE_COLUMN_INDEX),
                        row.getValue(DEVICE_TYPE_COLUMN_INDEX),
                        row.getValue(DIRECTION_COLUMN_INDEX),
                        row.getValue(FIRMWARE_VERSION_COLUMN_INDEX)
                ),
                ErrorCode.IMPORT_FILE_INVALID
        );
    }

    private ImportDevicePreviewResponse buildImportPreview(Operator operator, List<ImportDeviceRow> rows) {
        List<ImportDevicePreviewItem> items = rows.stream()
                .map(row -> validateImportRow(operator, row))
                .toList();
        List<ImportDeviceRowError> errors = items.stream()
                .flatMap(item -> item.getErrors().stream())
                .toList();

        return ImportDevicePreviewResponse.builder()
                .totalRows(rows.size())
                .validRows((int) items.stream().filter(ImportDevicePreviewItem::getValid).count())
                .invalidRows((int) items.stream().filter(item -> !item.getValid()).count())
                .items(items)
                .errors(errors)
                .build();
    }

    private ImportDevicePreviewItem validateImportRow(Operator operator, ImportDeviceRow row) {
        String stationCode = SearchFilterUtil.normalize(row.stationCode());
        String deviceType = SearchFilterUtil.normalizeUppercase(row.deviceType());
        String direction = SearchFilterUtil.normalizeUppercase(row.direction());
        String status = PredefinedDeviceStatus.ACTIVE;
        String firmwareVersion = SearchFilterUtil.normalize(row.firmwareVersion());
        List<ImportDeviceRowError> errors = new ArrayList<>();
        Station station = null;

        if (stationCode == null) {
            errors.add(importError(row.rowNumber(), STATION_CODE_HEADER, "Station code is required"));
        } else {
            Optional<Station> stationOptional = stationRepository.findByOperatorIdAndStationCode(
                    operator.getId(),
                    stationCode
            );
            if (stationOptional.isEmpty()) {
                errors.add(importError(row.rowNumber(), STATION_CODE_HEADER, "Station not found in current operator"));
            } else {
                station = stationOptional.get();
            }
        }

        if (deviceType == null) {
            errors.add(importError(row.rowNumber(), DEVICE_TYPE_HEADER, "Device type is required"));
        } else if (!PredefinedDeviceType.QR_SCANNER_SIMULATOR.equals(deviceType)) {
            errors.add(importError(row.rowNumber(), DEVICE_TYPE_HEADER, "Invalid device type"));
        }

        if (direction == null) {
            errors.add(importError(row.rowNumber(), DIRECTION_HEADER, "Direction is required"));
        } else if (!PredefinedDeviceDirection.ENTRY.equals(direction)
                && !PredefinedDeviceDirection.EXIT.equals(direction)
                && !PredefinedDeviceDirection.BOTH.equals(direction)) {
            errors.add(importError(row.rowNumber(), DIRECTION_HEADER, "Invalid direction"));
        }

        if (firmwareVersion != null && firmwareVersion.length() > MAX_FIRMWARE_VERSION_LENGTH) {
            errors.add(importError(row.rowNumber(), FIRMWARE_VERSION_HEADER,
                    "Firmware version must not exceed 100 characters"));
        }

        return ImportDevicePreviewItem.builder()
                .row(row.rowNumber())
                .stationId(station == null ? null : station.getId())
                .stationCode(stationCode)
                .stationName(station == null ? null : station.getStationName())
                .deviceType(deviceType)
                .direction(direction)
                .status(status)
                .firmwareVersion(firmwareVersion)
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private ImportDeviceRowError importError(Integer row, String field, String message) {
        return ImportDeviceRowError.builder()
                .row(row)
                .field(field)
                .message(message)
                .build();
    }

    private record ImportDeviceRow(Integer rowNumber, String stationCode, String deviceType,
                                   String direction, String firmwareVersion) {
    }
}
