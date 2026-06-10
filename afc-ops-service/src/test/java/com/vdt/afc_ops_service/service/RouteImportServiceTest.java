package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.dto.response.route.ImportRouteConfirmResponse;
import com.vdt.afc_ops_service.dto.response.route.ImportRoutePreviewResponse;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.repository.RouteRepository;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.Impl.RouteImportService;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteImportServiceTest {

    @Mock
    RouteRepository routeRepository;

    @Mock
    RouteCodeGenerator routeCodeGenerator;

    @Mock
    SecurityUtils securityUtils;

    @InjectMocks
    RouteImportService routeImportService;

    Operator operator;

    @BeforeEach
    void setUp() {
        operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        AfcUserDetails principal = AfcUserDetails.builder()
                .id("account-1")
                .username("manager")
                .operatorCode("HCMC-METRO")
                .authorities(List.of())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preview_ValidRows_NormalizesValues() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ImportRoutePreviewResponse response = routeImportService.preview(xlsx(
                row(" Metro Line 1 ", " metro "),
                row("Bus Route 01", "BUS")
        ));

        assertEquals(2, response.getTotalRows());
        assertEquals(2, response.getValidRows());
        assertEquals(0, response.getInvalidRows());
        assertTrue(response.getErrors().isEmpty());
        assertEquals("Metro Line 1", response.getItems().get(0).getRouteName());
        assertEquals(PredefinedTransportType.METRO, response.getItems().get(0).getTransportType());
    }

    @Test
    void preview_RouteImportTemplate_IsReadableAndValid() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        ClassPathResource template = new ClassPathResource("templates/route-import-template.xlsx");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "route-import-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                template.getInputStream()
        );

        ImportRoutePreviewResponse response = routeImportService.preview(file);

        assertEquals(2, response.getTotalRows());
        assertEquals(2, response.getValidRows());
        assertEquals(0, response.getInvalidRows());
    }

    @Test
    void preview_InvalidRows_ReturnsFieldErrors() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        ImportRoutePreviewResponse response = routeImportService.preview(xlsx(
                row("", "TRAIN"),
                row("x".repeat(256), "")
        ));

        assertEquals(0, response.getValidRows());
        assertEquals(2, response.getInvalidRows());
        assertEquals(4, response.getErrors().size());
    }

    @Test
    void preview_InvalidHeader_ThrowsImportFileInvalid() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        AppException exception = assertThrows(AppException.class,
                () -> routeImportService.preview(xlsxWithHeader("name", "type",
                        row("Metro Line 1", "METRO"))));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void confirm_InvalidRows_DoesNotPersist() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        AppException exception = assertThrows(AppException.class,
                () -> routeImportService.confirm(xlsx(row("", "METRO"))));

        assertEquals(ErrorCode.IMPORT_FILE_HAS_ERRORS, exception.getErrorCode());
        verify(routeRepository, never()).save(any());
    }

    @Test
    void confirm_ValidRows_CreatesRoutesForCurrentOperator() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(routeCodeGenerator.generate(operator, PredefinedTransportType.METRO)).thenReturn("METRO-001");
        when(routeCodeGenerator.generate(operator, PredefinedTransportType.BUS)).thenReturn("BUS-001");
        when(routeRepository.save(any(Route.class))).thenAnswer(invocation -> {
            Route route = invocation.getArgument(0);
            route.setId(PredefinedTransportType.METRO.equals(route.getTransportType()) ? 10L : 11L);
            return route;
        });

        ImportRouteConfirmResponse response = routeImportService.confirm(xlsx(
                row("Metro Line 1", "METRO"),
                row("Bus Route 01", "BUS")
        ));

        assertEquals(2, response.getImported());
        assertEquals("METRO-001", response.getItems().get(0).getRouteCode());

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(routeRepository, org.mockito.Mockito.times(2)).save(routeCaptor.capture());
        for (Route route : routeCaptor.getAllValues()) {
            assertEquals(operator, route.getOperator());
            assertEquals("account-1", route.getCreatedByAccountId());
            assertEquals(PredefinedMasterDataStatus.ACTIVE, route.getStatus());
        }
    }

    private Object[] row(String routeName, String transportType) {
        return new Object[]{routeName, transportType};
    }

    private MockMultipartFile xlsx(Object[]... rows) throws IOException {
        return xlsxWithHeader("routeName", "transportType", rows);
    }

    private MockMultipartFile xlsxWithHeader(String routeNameHeader, String transportTypeHeader,
                                             Object[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("routes");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(routeNameHeader);
            header.createCell(1).setCellValue(transportTypeHeader);

            for (int index = 0; index < rows.length; index++) {
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue((String) rows[index][0]);
                row.createCell(1).setCellValue((String) rows[index][1]);
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
