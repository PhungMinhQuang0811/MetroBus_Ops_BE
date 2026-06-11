package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.ExcelUtil;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.dto.response.station.ImportStationConfirmResponse;
import com.vdt.afc_ops_service.dto.response.station.ImportStationItemResponse;
import com.vdt.afc_ops_service.dto.response.station.ImportStationPreviewItem;
import com.vdt.afc_ops_service.dto.response.station.ImportStationPreviewResponse;
import com.vdt.afc_ops_service.dto.response.station.ImportStationRowError;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.repository.RouteRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.generator.StationCodeGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StationImportService {

    static final int ROUTE_CODE_COLUMN_INDEX = 0;
    static final int STATION_NAME_COLUMN_INDEX = 1;
    static final int STATION_ORDER_COLUMN_INDEX = 2;
    static final int MAX_STATION_NAME_LENGTH = 255;
    static final String ROUTE_CODE_HEADER = "routeCode";
    static final String STATION_NAME_HEADER = "stationName";
    static final String STATION_ORDER_HEADER = "stationOrder";
    static final List<String> IMPORT_HEADERS = List.of(ROUTE_CODE_HEADER, STATION_NAME_HEADER, STATION_ORDER_HEADER);

    RouteRepository routeRepository;
    StationRepository stationRepository;
    StationCodeGenerator stationCodeGenerator;
    SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public ImportStationPreviewResponse preview(MultipartFile file) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        return buildImportPreview(operator, parseImportRows(file));
    }

    @Transactional
    public ImportStationConfirmResponse confirm(MultipartFile file) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        ImportStationPreviewResponse preview = buildImportPreview(operator, parseImportRows(file));
        if (preview.getInvalidRows() > 0) {
            throw new AppException(ErrorCode.IMPORT_FILE_HAS_ERRORS);
        }

        String accountId = SecurityUtils.getRequiredCurrentAccountId();
        List<ImportStationItemResponse> importedItems = new ArrayList<>();
        for (ImportStationPreviewItem item : preview.getItems()) {
            Route route = routeRepository.findByIdAndOperator(item.getRouteId(), operator)
                    .orElseThrow(() -> new AppException(ErrorCode.ROUTE_NOT_FOUND));
            Station station = Station.builder()
                    .route(route)
                    .stationCode(stationCodeGenerator.generate(route))
                    .stationName(item.getStationName())
                    .stationOrder(item.getStationOrder())
                    .status(PredefinedMasterDataStatus.ACTIVE)
                    .createdByAccountId(accountId)
                    .build();

            Station savedStation = stationRepository.save(station);
            importedItems.add(ImportStationItemResponse.builder()
                    .row(item.getRow())
                    .id(savedStation.getId())
                    .routeId(route.getId())
                    .routeCode(route.getRouteCode())
                    .stationCode(savedStation.getStationCode())
                    .stationName(savedStation.getStationName())
                    .stationOrder(savedStation.getStationOrder())
                    .status(savedStation.getStatus())
                    .build());
        }

        return ImportStationConfirmResponse.builder()
                .imported(importedItems.size())
                .items(importedItems)
                .build();
    }

    private List<ImportStationRow> parseImportRows(MultipartFile file) {
        return ExcelUtil.parseRows(
                file,
                IMPORT_HEADERS,
                row -> new ImportStationRow(
                        row.rowNumber(),
                        row.getValue(ROUTE_CODE_COLUMN_INDEX),
                        row.getValue(STATION_NAME_COLUMN_INDEX),
                        row.getValue(STATION_ORDER_COLUMN_INDEX)
                ),
                ErrorCode.IMPORT_FILE_INVALID
        );
    }

    private ImportStationPreviewResponse buildImportPreview(Operator operator, List<ImportStationRow> rows) {
        Set<String> importedRouteOrders = new HashSet<>();
        List<ImportStationPreviewItem> items = rows.stream()
                .map(row -> validateImportRow(operator, row, importedRouteOrders))
                .toList();
        List<ImportStationRowError> errors = items.stream()
                .flatMap(item -> item.getErrors().stream())
                .toList();

        return ImportStationPreviewResponse.builder()
                .totalRows(rows.size())
                .validRows((int) items.stream().filter(ImportStationPreviewItem::getValid).count())
                .invalidRows((int) items.stream().filter(item -> !item.getValid()).count())
                .items(items)
                .errors(errors)
                .build();
    }

    private ImportStationPreviewItem validateImportRow(Operator operator, ImportStationRow row,
                                                       Set<String> importedRouteOrders) {
        String routeCode = SearchFilterUtil.normalize(row.routeCode());
        String stationName = SearchFilterUtil.normalize(row.stationName());
        Integer stationOrder = parseStationOrder(row.stationOrder());
        List<ImportStationRowError> errors = new ArrayList<>();
        Route route = null;

        if (routeCode == null) {
            errors.add(importError(row.rowNumber(), ROUTE_CODE_HEADER, "Route code is required"));
        } else {
            Optional<Route> routeOptional = routeRepository.findByOperatorAndRouteCode(operator, routeCode);
            if (routeOptional.isEmpty()) {
                errors.add(importError(row.rowNumber(), ROUTE_CODE_HEADER, "Route not found in current operator"));
            } else {
                route = routeOptional.get();
            }
        }

        if (stationName == null) {
            errors.add(importError(row.rowNumber(), STATION_NAME_HEADER, "Station name is required"));
        } else if (stationName.length() > MAX_STATION_NAME_LENGTH) {
            errors.add(importError(row.rowNumber(), STATION_NAME_HEADER,
                    "Station name must not exceed 255 characters"));
        }

        if (stationOrder == null || stationOrder < 1) {
            errors.add(importError(row.rowNumber(), STATION_ORDER_HEADER,
                    "Station order must be greater than or equal to 1"));
        } else if (route != null) {
            String routeOrderKey = route.getId() + ":" + stationOrder;
            if (!importedRouteOrders.add(routeOrderKey)
                    || stationRepository.existsByRouteAndStationOrder(route, stationOrder)) {
                errors.add(importError(row.rowNumber(), STATION_ORDER_HEADER,
                        "Station order already exists in route"));
            }
        }

        return ImportStationPreviewItem.builder()
                .row(row.rowNumber())
                .routeId(route == null ? null : route.getId())
                .routeCode(routeCode)
                .stationName(stationName)
                .stationOrder(stationOrder)
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private Integer parseStationOrder(String value) {
        String normalizedValue = SearchFilterUtil.normalize(value);
        if (normalizedValue == null) {
            return null;
        }
        try {
            return Integer.valueOf(normalizedValue);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private ImportStationRowError importError(Integer row, String field, String message) {
        return ImportStationRowError.builder()
                .row(row)
                .field(field)
                .message(message)
                .build();
    }

    private record ImportStationRow(Integer rowNumber, String routeCode, String stationName, String stationOrder) {
    }
}
