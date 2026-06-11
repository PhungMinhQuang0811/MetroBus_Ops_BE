package com.vdt.afc_ops_service.service;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedDeviceStatus;
import com.vdt.afc_ops_service.constant.PredefinedDeviceType;
import com.vdt.afc_ops_service.dto.response.device.ImportDeviceConfirmResponse;
import com.vdt.afc_ops_service.dto.response.device.ImportDevicePreviewResponse;
import com.vdt.afc_ops_service.entity.Device;
import com.vdt.afc_ops_service.entity.Operator;
import com.vdt.afc_ops_service.entity.Route;
import com.vdt.afc_ops_service.entity.Station;
import com.vdt.afc_ops_service.repository.DeviceRepository;
import com.vdt.afc_ops_service.repository.StationRepository;
import com.vdt.afc_ops_service.security.entity.AfcUserDetails;
import com.vdt.afc_ops_service.security.util.SecurityUtils;
import com.vdt.afc_ops_service.service.generator.DeviceCodeGenerator;
import com.vdt.afc_ops_service.service.Impl.DeviceImportService;
import com.vdt.afc_ops_service.service.generator.DeviceSecretGenerator;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceImportServiceTest {

    @Mock
    StationRepository stationRepository;

    @Mock
    DeviceRepository deviceRepository;

    @Mock
    DeviceCodeGenerator deviceCodeGenerator;

    @Mock
    DeviceSecretGenerator deviceSecretGenerator;

    @Mock
    SecurityUtils securityUtils;

    @InjectMocks
    DeviceImportService deviceImportService;

    Operator operator;
    Route route;
    Station station;

    @BeforeEach
    void setUp() {
        operator = Operator.builder().id(1L).operatorCode("HCMC-METRO").build();
        route = Route.builder().id(10L).operator(operator).routeCode("METRO-001").build();
        station = Station.builder()
                .id(100L)
                .route(route)
                .stationCode("METRO-001-ST-001")
                .stationName("Ben Thanh")
                .build();
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
        when(stationRepository.findByOperatorIdAndStationCode(1L, "METRO-001-ST-001"))
                .thenReturn(Optional.of(station));

        ImportDevicePreviewResponse response = deviceImportService.preview(xlsx(
                row(" METRO-001-ST-001 ", " qr_scanner_simulator ", " entry ", " 1.0.0 ")
        ));

        assertEquals(1, response.getTotalRows());
        assertEquals(1, response.getValidRows());
        assertEquals(PredefinedDeviceType.QR_SCANNER_SIMULATOR, response.getItems().get(0).getDeviceType());
        assertEquals(PredefinedDeviceStatus.ACTIVE, response.getItems().get(0).getStatus());
    }

    @Test
    void preview_DeviceImportTemplate_IsReadableAndValid() throws IOException {
        Station station2 = Station.builder()
                .id(101L)
                .route(route)
                .stationCode("METRO-001-ST-002")
                .stationName("Opera House")
                .build();
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(stationRepository.findByOperatorIdAndStationCode(1L, "METRO-001-ST-001"))
                .thenReturn(Optional.of(station));
        when(stationRepository.findByOperatorIdAndStationCode(1L, "METRO-001-ST-002"))
                .thenReturn(Optional.of(station2));
        ClassPathResource template = new ClassPathResource("templates/device-import-template.xlsx");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "device-import-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                template.getInputStream()
        );

        ImportDevicePreviewResponse response = deviceImportService.preview(file);

        assertEquals(2, response.getTotalRows());
        assertEquals(2, response.getValidRows());
        assertEquals(0, response.getInvalidRows());
        assertEquals("METRO-001-ST-001", response.getItems().get(0).getStationCode());
        assertEquals("METRO-001-ST-002", response.getItems().get(1).getStationCode());
    }

    @Test
    void preview_InvalidRows_ReturnsFieldErrors() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(stationRepository.findByOperatorIdAndStationCode(1L, "MISSING")).thenReturn(Optional.empty());

        ImportDevicePreviewResponse response = deviceImportService.preview(xlsx(
                row("", "", "", ""),
                row("MISSING", "BAD_TYPE", "BAD_DIRECTION", "1.0.0")
        ));

        assertEquals(0, response.getValidRows());
        assertEquals(1, response.getInvalidRows());
        assertEquals(3, response.getErrors().size());
    }

    @Test
    void preview_MultipleRowsForSameStation_AreValidBecauseDeviceCodesAreGenerated() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(stationRepository.findByOperatorIdAndStationCode(1L, "METRO-001-ST-001"))
                .thenReturn(Optional.of(station));

        ImportDevicePreviewResponse response = deviceImportService.preview(xlsx(
                row("METRO-001-ST-001", "QR_SCANNER_SIMULATOR", "ENTRY", "1.0.0"),
                row("METRO-001-ST-001", "QR_SCANNER_SIMULATOR", "EXIT", "1.0.0")
        ));

        assertEquals(2, response.getValidRows());
        assertEquals(0, response.getInvalidRows());
    }

    @Test
    void confirm_InvalidRows_DoesNotPersist() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);

        AppException exception = assertThrows(AppException.class,
                () -> deviceImportService.confirm(xlsx(row("", "QR_SCANNER_SIMULATOR",
                        "ENTRY", "1.0.0"))));

        assertEquals(ErrorCode.IMPORT_FILE_HAS_ERRORS, exception.getErrorCode());
        verify(deviceRepository, never()).save(any());
    }

    @Test
    void confirm_ValidRows_CreatesDevicesForCurrentOperator() throws IOException {
        when(securityUtils.getRequiredCurrentOperator()).thenReturn(operator);
        when(stationRepository.findByOperatorIdAndStationCode(1L, "METRO-001-ST-001"))
                .thenReturn(Optional.of(station));
        when(stationRepository.findByIdAndRouteOperatorId(100L, 1L)).thenReturn(Optional.of(station));
        when(deviceCodeGenerator.generate(station)).thenReturn("METRO-001-ST-001-DV-001");
        when(deviceSecretGenerator.generate()).thenReturn("generated-secret");
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> {
            Device device = invocation.getArgument(0);
            device.setId(200L);
            return device;
        });

        ImportDeviceConfirmResponse response = deviceImportService.confirm(xlsx(
                row("METRO-001-ST-001", "QR_SCANNER_SIMULATOR", "ENTRY", "1.0.0")
        ));

        assertEquals(1, response.getImported());
        assertEquals("METRO-001-ST-001-DV-001", response.getItems().get(0).getDeviceCode());
        assertEquals("generated-secret", response.getItems().get(0).getDeviceSecret());

        ArgumentCaptor<Device> deviceCaptor = ArgumentCaptor.forClass(Device.class);
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertEquals(station, savedDevice.getStation());
        assertEquals("account-1", savedDevice.getCreatedByAccountId());
        assertEquals(PredefinedDeviceStatus.ACTIVE, savedDevice.getStatus());
    }

    private Object[] row(String stationCode, String deviceType, String direction, String firmwareVersion) {
        return new Object[]{stationCode, deviceType, direction, firmwareVersion};
    }

    private MockMultipartFile xlsx(Object[]... rows) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("devices");
            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("stationCode");
            header.createCell(1).setCellValue("deviceType");
            header.createCell(2).setCellValue("direction");
            header.createCell(3).setCellValue("firmwareVersion");

            for (int index = 0; index < rows.length; index++) {
                Row row = sheet.createRow(index + 1);
                row.createCell(0).setCellValue((String) rows[index][0]);
                row.createCell(1).setCellValue((String) rows[index][1]);
                row.createCell(2).setCellValue((String) rows[index][2]);
                row.createCell(3).setCellValue((String) rows[index][3]);
            }

            workbook.write(outputStream);
            return new MockMultipartFile(
                    "file",
                    "devices.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    outputStream.toByteArray()
            );
        }
    }
}
