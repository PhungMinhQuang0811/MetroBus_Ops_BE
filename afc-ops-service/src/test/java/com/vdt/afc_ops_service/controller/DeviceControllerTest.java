package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedDeviceStatus;
import com.vdt.afc_ops_service.constant.PredefinedDeviceType;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceDetailResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceProvisionResponse;
import com.vdt.afc_ops_service.dto.response.device.DeviceResponse;
import com.vdt.afc_ops_service.dto.response.device.ImportDeviceConfirmResponse;
import com.vdt.afc_ops_service.dto.response.device.ImportDeviceItemResponse;
import com.vdt.afc_ops_service.dto.response.device.ImportDevicePreviewItem;
import com.vdt.afc_ops_service.dto.response.device.ImportDevicePreviewResponse;
import com.vdt.afc_ops_service.service.IDeviceService;
import com.vdt.afc_ops_service.service.Impl.DeviceImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DeviceControllerTest {

    @Mock
    IDeviceService deviceService;

    @Mock
    DeviceImportService deviceImportService;

    MockMvc mockMvc;
    DeviceController deviceController;

    @BeforeEach
    void setUp() {
        deviceController = new DeviceController(deviceService, deviceImportService);
        mockMvc = MockMvcBuilders.standaloneSetup(deviceController).build();
    }

    @Test
    void listDevices_UsesDeviceBasePath() throws Exception {
        when(deviceService.listDevices(1L, PredefinedDeviceType.QR_SCANNER_SIMULATOR,
                PredefinedDeviceStatus.ACTIVE, "gate", 0, 20))
                .thenReturn(PageResponse.<DeviceResponse>builder()
                        .items(List.of(sampleDevice()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/device/list-devices")
                        .param("stationId", "1")
                        .param("deviceType", PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                        .param("status", PredefinedDeviceStatus.ACTIVE)
                        .param("keyword", "gate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].deviceCode").value("GATE-001"));
    }

    @Test
    void getDevice_UsesDeviceBasePath() throws Exception {
        when(deviceService.getDevice(1L)).thenReturn(DeviceDetailResponse.builder()
                .id(1L)
                .deviceCode("GATE-001")
                .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                .direction("ENTRY")
                .status(PredefinedDeviceStatus.ACTIVE)
                .stationId(1L)
                .stationCode("METRO-001-ST-001")
                .stationName("Ben Thanh")
                .routeId(1L)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .firmwareVersion("1.0.0")
                .latestIncident(null)
                .build());

        mockMvc.perform(get("/device/get-device/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.deviceCode").value("GATE-001"))
                .andExpect(jsonPath("$.result.stationName").value("Ben Thanh"))
                .andExpect(jsonPath("$.result.routeName").value("Metro Line 1"))
                .andExpect(jsonPath("$.result.firmwareVersion").value("1.0.0"));
    }

    @Test
    void createDevice_UsesDeviceBasePath() throws Exception {
        when(deviceService.createDevice(any())).thenReturn(sampleProvisionDevice());

        mockMvc.perform(post("/device/create-device")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": 1,
                                  "deviceType": "QR_SCANNER_SIMULATOR",
                                  "direction": "ENTRY",
                                  "firmwareVersion": "1.0.0"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.deviceCode").value("GATE-001"))
                .andExpect(jsonPath("$.result.deviceSecret").value("generated-secret"));
    }

    @Test
    void updateDevice_UsesDeviceBasePath() throws Exception {
        when(deviceService.updateDevice(any(), any())).thenReturn(sampleDevice());

        mockMvc.perform(post("/device/update-device/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": 1,
                                  "deviceType": "QR_SCANNER_SIMULATOR",
                                  "direction": "BOTH",
                                  "status": "MAINTENANCE",
                                  "firmwareVersion": "1.0.1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.deviceCode").value("GATE-001"));
    }

    @Test
    void enableAndDisableDevice_UseDeviceBasePath() throws Exception {
        when(deviceService.enableDevice(1L)).thenReturn(sampleDevice());
        DeviceResponse disabledDevice = sampleDevice();
        disabledDevice.setStatus(PredefinedDeviceStatus.DISABLED);
        when(deviceService.disableDevice(1L)).thenReturn(disabledDevice);

        mockMvc.perform(post("/device/enable-device/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));
        mockMvc.perform(post("/device/disable-device/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("DISABLED"));
    }

    @Test
    void previewAndConfirmImportDevices_UseDeviceBasePath() throws Exception {
        when(deviceImportService.preview(any())).thenReturn(ImportDevicePreviewResponse.builder()
                .totalRows(1)
                .validRows(1)
                .invalidRows(0)
                .items(List.of(ImportDevicePreviewItem.builder()
                        .row(2)
                        .stationCode("METRO-001-ST-001")
                        .valid(true)
                        .errors(List.of())
                        .build()))
                .errors(List.of())
                .build());
        when(deviceImportService.confirm(any())).thenReturn(ImportDeviceConfirmResponse.builder()
                .imported(1)
                .items(List.of(ImportDeviceItemResponse.builder()
                        .row(2)
                        .id(1L)
                        .deviceCode("GATE-001")
                        .build()))
                .build());

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "device-import-template.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes()
        );
        mockMvc.perform(multipart("/device/preview-import-devices").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalRows").value(1));
        mockMvc.perform(multipart("/device/confirm-import-devices").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.imported").value(1));
    }

    @Test
    void previewImportDevices_MissingFile_ThrowsImportFileInvalid() {
        AppException exception = assertThrows(AppException.class,
                () -> deviceController.previewImportDevices(null));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void previewImportDevices_MultipleFiles_ThrowsImportFileInvalid() {
        MockMultipartFile firstFile = new MockMultipartFile("file", "first.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "1".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("file", "second.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "2".getBytes());

        AppException exception = assertThrows(AppException.class,
                () -> deviceController.previewImportDevices(List.of(firstFile, secondFile)));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void confirmImportDevices_MissingFile_ThrowsImportFileInvalid() {
        AppException exception = assertThrows(AppException.class,
                () -> deviceController.confirmImportDevices(null));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void confirmImportDevices_MultipleFiles_ThrowsImportFileInvalid() {
        MockMultipartFile firstFile = new MockMultipartFile("file", "first.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "1".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("file", "second.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "2".getBytes());

        AppException exception = assertThrows(AppException.class,
                () -> deviceController.confirmImportDevices(List.of(firstFile, secondFile)));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    private DeviceResponse sampleDevice() {
        return DeviceResponse.builder()
                .id(1L)
                .routeId(1L)
                .routeCode("METRO-001")
                .stationId(1L)
                .stationCode("METRO-001-ST-001")
                .stationName("Ben Thanh")
                .deviceCode("GATE-001")
                .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                .direction("ENTRY")
                .status(PredefinedDeviceStatus.ACTIVE)
                .firmwareVersion("1.0.0")
                .createdAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .build();
    }

    private DeviceProvisionResponse sampleProvisionDevice() {
        return DeviceProvisionResponse.builder()
                .id(1L)
                .routeId(1L)
                .routeCode("METRO-001")
                .stationId(1L)
                .stationCode("METRO-001-ST-001")
                .stationName("Ben Thanh")
                .deviceCode("GATE-001")
                .deviceType(PredefinedDeviceType.QR_SCANNER_SIMULATOR)
                .direction("ENTRY")
                .status(PredefinedDeviceStatus.ACTIVE)
                .firmwareVersion("1.0.0")
                .deviceSecret("generated-secret")
                .createdAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .build();
    }
}
