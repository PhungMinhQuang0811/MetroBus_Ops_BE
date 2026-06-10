package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.station.ImportStationConfirmResponse;
import com.vdt.afc_ops_service.dto.response.station.ImportStationItemResponse;
import com.vdt.afc_ops_service.dto.response.station.ImportStationPreviewItem;
import com.vdt.afc_ops_service.dto.response.station.ImportStationPreviewResponse;
import com.vdt.afc_ops_service.dto.response.station.StationResponse;
import com.vdt.afc_ops_service.dto.response.station.StationDetailResponse;
import com.vdt.afc_ops_service.dto.response.station.StationDeviceResponse;
import com.vdt.afc_ops_service.dto.response.station.StationDeviceSummary;
import com.vdt.afc_ops_service.service.IStationService;
import com.vdt.afc_ops_service.service.Impl.StationImportService;
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
class StationControllerTest {

    @Mock
    IStationService stationService;

    @Mock
    StationImportService stationImportService;

    MockMvc mockMvc;
    StationController stationController;

    @BeforeEach
    void setUp() {
        stationController = new StationController(stationService, stationImportService);
        mockMvc = MockMvcBuilders.standaloneSetup(stationController).build();
    }

    @Test
    void listStations_UsesStationBasePath() throws Exception {
        when(stationService.listStations(1L, "ben", "ACTIVE", 0, 20))
                .thenReturn(PageResponse.<StationResponse>builder()
                        .items(List.of(sampleStation()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/station/list-stations")
                        .param("routeId", "1")
                        .param("keyword", "ben")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].stationCode").value("METRO-001-ST-001"));
    }

    @Test
    void getStation_UsesStationBasePath() throws Exception {
        when(stationService.getStation(1L)).thenReturn(StationDetailResponse.builder()
                .id(1L)
                .routeId(1L)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .stationCode("METRO-001-ST-001")
                .stationName("Ben Thanh")
                .stationOrder(1)
                .status(PredefinedMasterDataStatus.ACTIVE)
                .deviceSummary(StationDeviceSummary.builder()
                        .total(1)
                        .active(1)
                        .offline(0)
                        .maintenance(0)
                        .disabled(0)
                        .build())
                .devices(List.of(StationDeviceResponse.builder()
                        .id(10L)
                        .deviceCode("GATE-001")
                        .deviceType("QR_SCANNER_SIMULATOR")
                        .direction("ENTRY")
                        .status("ACTIVE")
                        .build()))
                .build());

        mockMvc.perform(get("/station/get-station/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.routeName").value("Metro Line 1"))
                .andExpect(jsonPath("$.result.deviceSummary.total").value(1))
                .andExpect(jsonPath("$.result.devices[0].deviceCode").value("GATE-001"));
    }

    @Test
    void createStation_UsesStationBasePath() throws Exception {
        when(stationService.createStation(any())).thenReturn(sampleStation());

        mockMvc.perform(post("/station/create-station")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"routeId":1,"stationName":"Ben Thanh","stationOrder":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.stationCode").value("METRO-001-ST-001"));
    }

    @Test
    void updateStation_UsesStationBasePath() throws Exception {
        when(stationService.updateStation(any(), any())).thenReturn(sampleStation());

        mockMvc.perform(post("/station/update-station/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"routeId":1,"stationName":"Ben Thanh Updated","stationOrder":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.stationCode").value("METRO-001-ST-001"));
    }

    @Test
    void enableStation_UsesStationBasePath() throws Exception {
        when(stationService.enableStation(1L)).thenReturn(sampleStation());

        mockMvc.perform(post("/station/enable-station/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));
    }

    @Test
    void disableStation_UsesStationBasePath() throws Exception {
        StationResponse disabledStation = sampleStation();
        disabledStation.setStatus(PredefinedMasterDataStatus.DISABLED);
        when(stationService.disableStation(1L)).thenReturn(disabledStation);

        mockMvc.perform(post("/station/disable-station/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("DISABLED"));
    }

    @Test
    void previewImportStations_UsesStationBasePath() throws Exception {
        when(stationImportService.preview(any())).thenReturn(samplePreview());

        mockMvc.perform(multipart("/station/preview-import-stations")
                        .file(new MockMultipartFile(
                                "file",
                                "station-import-template.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "dummy".getBytes()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalRows").value(1))
                .andExpect(jsonPath("$.result.items[0].routeCode").value("METRO-001"));
    }

    @Test
    void confirmImportStations_UsesStationBasePath() throws Exception {
        when(stationImportService.confirm(any())).thenReturn(sampleConfirm());

        mockMvc.perform(multipart("/station/confirm-import-stations")
                        .file(new MockMultipartFile(
                                "file",
                                "station-import-template.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "dummy".getBytes()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.imported").value(1))
                .andExpect(jsonPath("$.result.items[0].stationCode").value("METRO-001-ST-001"));
    }

    @Test
    void previewImportStations_MissingFile_ThrowsImportFileInvalid() {
        AppException exception = assertThrows(AppException.class, () -> stationController.previewImportStations(null));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void previewImportStations_MultipleFiles_ThrowsImportFileInvalid() {
        MockMultipartFile firstFile = new MockMultipartFile("file", "first.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "1".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("file", "second.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "2".getBytes());

        AppException exception = assertThrows(AppException.class,
                () -> stationController.previewImportStations(List.of(firstFile, secondFile)));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void confirmImportStations_MissingFile_ThrowsImportFileInvalid() {
        AppException exception = assertThrows(AppException.class, () -> stationController.confirmImportStations(null));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void confirmImportStations_MultipleFiles_ThrowsImportFileInvalid() {
        MockMultipartFile firstFile = new MockMultipartFile("file", "first.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "1".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("file", "second.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "2".getBytes());

        AppException exception = assertThrows(AppException.class,
                () -> stationController.confirmImportStations(List.of(firstFile, secondFile)));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    private StationResponse sampleStation() {
        return StationResponse.builder()
                .id(1L)
                .routeId(1L)
                .routeCode("METRO-001")
                .stationCode("METRO-001-ST-001")
                .stationName("Ben Thanh")
                .stationOrder(1)
                .status(PredefinedMasterDataStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .build();
    }

    private ImportStationPreviewResponse samplePreview() {
        return ImportStationPreviewResponse.builder()
                .totalRows(1)
                .validRows(1)
                .invalidRows(0)
                .items(List.of(ImportStationPreviewItem.builder()
                        .row(2)
                        .routeId(1L)
                        .routeCode("METRO-001")
                        .stationName("Ben Thanh")
                        .stationOrder(1)
                        .valid(true)
                        .errors(List.of())
                        .build()))
                .errors(List.of())
                .build();
    }

    private ImportStationConfirmResponse sampleConfirm() {
        return ImportStationConfirmResponse.builder()
                .imported(1)
                .items(List.of(ImportStationItemResponse.builder()
                        .row(2)
                        .id(1L)
                        .routeId(1L)
                        .routeCode("METRO-001")
                        .stationCode("METRO-001-ST-001")
                        .stationName("Ben Thanh")
                        .stationOrder(1)
                        .status(PredefinedMasterDataStatus.ACTIVE)
                        .build()))
                .build();
    }
}
