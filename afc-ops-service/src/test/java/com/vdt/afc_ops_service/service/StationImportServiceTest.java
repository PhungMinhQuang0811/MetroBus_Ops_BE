package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.dto.response.station.ImportStationConfirmResponse;
import com.vdt.afc_ops_service.dto.response.station.ImportStationPreviewResponse;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.repository.RouteRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.Impl.StationImportService;
import com.vdt.afc_ops_service.service.generator.StationCodeGenerator;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StationImportServiceTest {

    @Mock
    RouteRepository routeRepository;

    @Mock
    StationRepository stationRepository;

    @Mock
    StationCodeGenerator stationCodeGenerator;

    @Mock
    SecurityUtils securityUtils;

    @InjectMocks
    StationImportService stationImportService;

    Operator operator;
    Route route;

    @BeforeEach
    void setUp() {
        operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        route = Route.builder().id(10L).operator(operator).routeCode("METRO-001").build();
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
        when(routeRepository.findByOperatorAndRouteCode(operator, "METRO-001")).thenReturn(Optional.of(route));
        when(stationRepository.existsByRouteAndStationOrder(eq(route), anyInt())).thenReturn(false);

        ImportStationPreviewResponse response = stationImportService.preview(xlsx(
                row(" METRO-001 ", " Ben Thanh ", "1"),
                row("METRO-001", "Opera House", "2")
        ));

        assertEquals(2, response.getTotalRows());
        assertEquals(2, response.getValidRows());
        assertEquals(0, response.getInvalidRows());
        assertTrue(response.getErrors().isEmpty());
        assertEquals("Ben Thanh", response.getItems().get(0).getStationName());
    }

    @Test
    void preview_StationImportTemplate_IsReadableAndValid() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(routeRepository.findByOperatorAndRouteCode(operator, "METRO-001")).thenReturn(Optional.of(route));
        when(stationRepository.existsByRouteAndStationOrder(eq(route), anyInt())).thenReturn(false);
        ClassPathResource template = new ClassPathResource("templates/station-import-template.xlsx");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "station-import-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                template.getInputStream()
        );

        ImportStationPreviewResponse response = stationImportService.preview(file);

        assertEquals(2, response.getTotalRows());
        assertEquals(2, response.getValidRows());
        assertEquals(0, response.getInvalidRows());
        assertTrue(response.getErrors().isEmpty());
        assertEquals("METRO-001", response.getItems().get(0).getRouteCode());
        assertEquals("Ben Thanh", response.getItems().get(0).getStationName());
        assertEquals(2, response.getItems().get(0).getStationOrder());
        assertEquals("Opera House", response.getItems().get(1).getStationName());
        assertEquals(3, response.getItems().get(1).getStationOrder());
    }

    @Test
    void preview_InvalidRows_ReturnsFieldErrors() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(routeRepository.findByOperatorAndRouteCode(operator, "MISSING")).thenReturn(Optional.empty());
        when(routeRepository.findByOperatorAndRouteCode(operator, "METRO-001")).thenReturn(Optional.of(route));
        when(stationRepository.existsByRouteAndStationOrder(route, 1)).thenReturn(true);

        ImportStationPreviewResponse response = stationImportService.preview(xlsx(
                row("", "", "0"),
                row("MISSING", "Station", "abc"),
                row("METRO-001", "Existing", "1")
        ));

        assertEquals(0, response.getValidRows());
        assertEquals(3, response.getInvalidRows());
        assertEquals(6, response.getErrors().size());
    }

    @Test
    void preview_DuplicateOrdersInFile_ReturnsError() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(routeRepository.findByOperatorAndRouteCode(operator, "METRO-001")).thenReturn(Optional.of(route));
        when(stationRepository.existsByRouteAndStationOrder(route, 1)).thenReturn(false);

        ImportStationPreviewResponse response = stationImportService.preview(xlsx(
                row("METRO-001", "Ben Thanh", "1"),
                row("METRO-001", "Opera House", "1")
        ));

        assertEquals(1, response.getValidRows());
        assertEquals(1, response.getInvalidRows());
    }

    @Test
    void confirm_InvalidRows_DoesNotPersist() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        AppException exception = assertThrows(AppException.class,
                () -> stationImportService.confirm(xlsx(row("", "Ben Thanh", "1"))));

        assertEquals(ErrorCode.IMPORT_FILE_HAS_ERRORS, exception.getErrorCode());
        verify(stationRepository, never()).save(any());
    }

    @Test
    void confirm_ValidRows_CreatesStationsForCurrentOperator() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(routeRepository.findByOperatorAndRouteCode(operator, "METRO-001")).thenReturn(Optional.of(route));
        when(routeRepository.findByIdAndOperator(10L, operator)).thenReturn(Optional.of(route));
        when(stationRepository.existsByRouteAndStationOrder(route, 1)).thenReturn(false);
        when(stationRepository.existsByRouteAndStationOrder(route, 2)).thenReturn(false);
        when(stationCodeGenerator.generate(route)).thenReturn("METRO-001-ST-001", "METRO-001-ST-002");
        when(stationRepository.save(any(Station.class))).thenAnswer(invocation -> {
            Station station = invocation.getArgument(0);
            station.setId(station.getStationOrder().longValue());
            return station;
        });

        ImportStationConfirmResponse response = stationImportService.confirm(xlsx(
                row("METRO-001", "Ben Thanh", "1"),
                row("METRO-001", "Opera House", "2")
        ));

        assertEquals(2, response.getImported());
        assertEquals("METRO-001-ST-001", response.getItems().get(0).getStationCode());

        ArgumentCaptor<Station> stationCaptor = ArgumentCaptor.forClass(Station.class);
        verify(stationRepository, org.mockito.Mockito.times(2)).save(stationCaptor.capture());
        for (Station station : stationCaptor.getAllValues()) {
            assertEquals(route, station.getRoute());
            assertEquals("account-1", station.getCreatedByAccountId());
            assertEquals(PredefinedMasterDataStatus.ACTIVE, station.getStatus());
        }
    }

    private Object[] row(String routeCode, String stationName, String stationOrder) {
        return new Object[]{routeCode, stationName, stationOrder};
    }

    private MockMultipartFile xlsx(Object[]... rows) throws IOException {
        return xlsxWithHeader("routeCode", "stationName", "stationOrder", rows);
    }

    private MockMultipartFile xlsxWithHeader(String routeCodeHeader, String stationNameHeader,
                                             String stationOrderHeader, Object[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("stations");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue(routeCodeHeader);
            header.createCell(1).setCellValue(stationNameHeader);
            header.createCell(2).setCellValue(stationOrderHeader);

            for (int index = 0; index < rows.length; index++) {
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue((String) rows[index][0]);
                row.createCell(1).setCellValue((String) rows[index][1]);
                row.createCell(2).setCellValue((String) rows[index][2]);
            }

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "stations.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }
}
