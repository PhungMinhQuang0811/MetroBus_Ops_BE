package com.vdt.afc_ops_service.service.Impl;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.common.util.ExcelUtil;
import com.vdt.afc_ops_service.common.util.SearchFilterUtil;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.dto.response.route.ImportRouteConfirmResponse;
import com.vdt.afc_ops_service.dto.response.route.ImportRouteItemResponse;
import com.vdt.afc_ops_service.dto.response.route.ImportRoutePreviewItem;
import com.vdt.afc_ops_service.dto.response.route.ImportRoutePreviewResponse;
import com.vdt.afc_ops_service.dto.response.route.ImportRouteRowError;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.repository.RouteRepository;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.generator.RouteCodeGenerator;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RouteImportService {

    static final int ROUTE_NAME_COLUMN_INDEX = 0;
    static final int TRANSPORT_TYPE_COLUMN_INDEX = 1;
    static final int MAX_ROUTE_NAME_LENGTH = 255;
    static final String ROUTE_NAME_HEADER = "routeName";
    static final String TRANSPORT_TYPE_HEADER = "transportType";
    static final List<String> IMPORT_HEADERS = List.of(ROUTE_NAME_HEADER, TRANSPORT_TYPE_HEADER);

    RouteRepository routeRepository;
    RouteCodeGenerator routeCodeGenerator;
    SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public ImportRoutePreviewResponse preview(MultipartFile file) {
        securityUtils.getRequiredCurrentOperator();
        return buildImportPreview(parseImportRows(file));
    }

    @Transactional
    public ImportRouteConfirmResponse confirm(MultipartFile file) {
        Operator operator = securityUtils.getRequiredCurrentOperator();
        ImportRoutePreviewResponse preview = buildImportPreview(parseImportRows(file));
        if (preview.getInvalidRows() > 0) {
            throw new AppException(ErrorCode.IMPORT_FILE_HAS_ERRORS);
        }

        String accountId = SecurityUtils.getRequiredCurrentAccountId();
        List<ImportRouteItemResponse> importedItems = new ArrayList<>();
        for (ImportRoutePreviewItem item : preview.getItems()) {
            Route route = Route.builder()
                    .operator(operator)
                    .routeCode(routeCodeGenerator.generate(operator, item.getTransportType()))
                    .routeName(item.getRouteName())
                    .transportType(item.getTransportType())
                    .status(PredefinedMasterDataStatus.ACTIVE)
                    .createdByAccountId(accountId)
                    .build();

            Route savedRoute = routeRepository.save(route);
            importedItems.add(ImportRouteItemResponse.builder()
                    .row(item.getRow())
                    .id(savedRoute.getId())
                    .operatorId(operator.getId())
                    .routeCode(savedRoute.getRouteCode())
                    .routeName(savedRoute.getRouteName())
                    .transportType(savedRoute.getTransportType())
                    .status(savedRoute.getStatus())
                    .build());
        }

        return ImportRouteConfirmResponse.builder()
                .imported(importedItems.size())
                .items(importedItems)
                .build();
    }

    private List<ImportRouteRow> parseImportRows(MultipartFile file) {
        return ExcelUtil.parseRows(
                file,
                IMPORT_HEADERS,
                row -> new ImportRouteRow(
                        row.rowNumber(),
                        row.getValue(ROUTE_NAME_COLUMN_INDEX),
                        row.getValue(TRANSPORT_TYPE_COLUMN_INDEX)
                ),
                ErrorCode.IMPORT_FILE_INVALID
        );
    }

    private ImportRoutePreviewResponse buildImportPreview(List<ImportRouteRow> rows) {
        List<ImportRoutePreviewItem> items = rows.stream()
                .map(this::validateImportRow)
                .toList();
        List<ImportRouteRowError> errors = items.stream()
                .flatMap(item -> item.getErrors().stream())
                .toList();

        return ImportRoutePreviewResponse.builder()
                .totalRows(rows.size())
                .validRows((int) items.stream().filter(ImportRoutePreviewItem::getValid).count())
                .invalidRows((int) items.stream().filter(item -> !item.getValid()).count())
                .items(items)
                .errors(errors)
                .build();
    }

    private ImportRoutePreviewItem validateImportRow(ImportRouteRow row) {
        String routeName = SearchFilterUtil.normalize(row.routeName());
        String transportType = SearchFilterUtil.normalizeUppercase(row.transportType());
        List<ImportRouteRowError> errors = new ArrayList<>();

        if (routeName == null) {
            errors.add(importError(row.rowNumber(), ROUTE_NAME_HEADER, "Route name is required"));
        } else if (routeName.length() > MAX_ROUTE_NAME_LENGTH) {
            errors.add(importError(row.rowNumber(), ROUTE_NAME_HEADER,
                    "Route name must not exceed 255 characters"));
        }

        if (transportType == null) {
            errors.add(importError(row.rowNumber(), TRANSPORT_TYPE_HEADER, "Transport type is required"));
        } else if (!PredefinedTransportType.METRO.equals(transportType)
                && !PredefinedTransportType.BUS.equals(transportType)) {
            errors.add(importError(row.rowNumber(), TRANSPORT_TYPE_HEADER, "Invalid transport type"));
        }

        return ImportRoutePreviewItem.builder()
                .row(row.rowNumber())
                .routeName(routeName)
                .transportType(transportType)
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private ImportRouteRowError importError(Integer row, String field, String message) {
        return ImportRouteRowError.builder()
                .row(row)
                .field(field)
                .message(message)
                .build();
    }

    private record ImportRouteRow(Integer rowNumber, String routeName, String transportType) {
    }
}
