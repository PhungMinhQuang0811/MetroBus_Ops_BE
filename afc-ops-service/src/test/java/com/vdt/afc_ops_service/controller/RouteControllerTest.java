package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.common.exception.AppException;
import com.vdt.afc_ops_service.common.exception.ErrorCode;
import com.vdt.afc_ops_service.constant.PredefinedMasterDataStatus;
import com.vdt.afc_ops_service.constant.PredefinedTransportType;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.route.ImportRouteConfirmResponse;
import com.vdt.afc_ops_service.dto.response.route.ImportRouteItemResponse;
import com.vdt.afc_ops_service.dto.response.route.ImportRoutePreviewItem;
import com.vdt.afc_ops_service.dto.response.route.ImportRoutePreviewResponse;
import com.vdt.afc_ops_service.dto.response.route.RouteResponse;
import com.vdt.afc_ops_service.service.IRouteService;
import com.vdt.afc_ops_service.service.Impl.RouteImportService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RouteControllerTest {

    @Mock
    IRouteService routeService;

    @Mock
    RouteImportService routeImportService;

    MockMvc mockMvc;
    RouteController routeController;

    @BeforeEach
    void setUp() {
        routeController = new RouteController(routeService, routeImportService);
        mockMvc = MockMvcBuilders.standaloneSetup(routeController).build();
    }

    @Test
    void listRoutes_UsesRouteBasePath() throws Exception {
        when(routeService.listRoutes("metro", "METRO", "ACTIVE", 0, 20)).thenReturn(PageResponse.<RouteResponse>builder()
                .items(List.of(sampleRoute()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .build());

        mockMvc.perform(get("/route/list-routes")
                        .param("keyword", "metro")
                        .param("transportType", "METRO")
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].routeCode").value("METRO-001"));
    }

    @Test
    void createRoute_UsesRouteBasePath() throws Exception {
        when(routeService.createRoute(any())).thenReturn(sampleRoute());

        mockMvc.perform(post("/route/create-route")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"routeName":"Metro Line 1","transportType":"METRO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.routeCode").value("METRO-001"));
    }

    @Test
    void updateRoute_UsesRouteBasePath() throws Exception {
        when(routeService.updateRoute(any(), any())).thenReturn(sampleRoute());

        mockMvc.perform(post("/route/update-route/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"routeName":"Metro Line 1","transportType":"METRO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.routeCode").value("METRO-001"));
    }

    @Test
    void enableRoute_UsesRouteBasePath() throws Exception {
        when(routeService.enableRoute(1L)).thenReturn(sampleRoute());

        mockMvc.perform(post("/route/enable-route/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("ACTIVE"));
    }

    @Test
    void disableRoute_UsesRouteBasePath() throws Exception {
        RouteResponse disabledRoute = sampleRoute();
        disabledRoute.setStatus(PredefinedMasterDataStatus.DISABLED);
        when(routeService.disableRoute(1L)).thenReturn(disabledRoute);

        mockMvc.perform(post("/route/disable-route/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("DISABLED"));
    }

    @Test
    void previewImportRoutes_UsesRouteBasePath() throws Exception {
        when(routeImportService.preview(any())).thenReturn(samplePreview());

        mockMvc.perform(multipart("/route/preview-import-routes")
                        .file(new MockMultipartFile(
                                "file",
                                "route-import-template.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "dummy".getBytes()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalRows").value(1))
                .andExpect(jsonPath("$.result.items[0].routeName").value("Metro Line 1"));
    }

    @Test
    void confirmImportRoutes_UsesRouteBasePath() throws Exception {
        when(routeImportService.confirm(any())).thenReturn(sampleConfirm());

        mockMvc.perform(multipart("/route/confirm-import-routes")
                        .file(new MockMultipartFile(
                                "file",
                                "route-import-template.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "dummy".getBytes()
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.imported").value(1))
                .andExpect(jsonPath("$.result.items[0].routeCode").value("METRO-001"));
    }

    @Test
    void previewImportRoutes_MissingFile_ThrowsImportFileInvalid() {
        AppException exception = assertThrows(AppException.class, () -> routeController.previewImportRoutes(null));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    @Test
    void previewImportRoutes_MultipleFiles_ThrowsImportFileInvalid() {
        MockMultipartFile firstFile = new MockMultipartFile("file", "first.xlsx", "application/vnd.ms-excel", "1".getBytes());
        MockMultipartFile secondFile = new MockMultipartFile("file", "second.xlsx", "application/vnd.ms-excel", "2".getBytes());

        AppException exception = assertThrows(AppException.class,
                () -> routeController.previewImportRoutes(List.of(firstFile, secondFile)));

        assertEquals(ErrorCode.IMPORT_FILE_INVALID, exception.getErrorCode());
    }

    private RouteResponse sampleRoute() {
        return RouteResponse.builder()
                .id(1L)
                .operatorId(1L)
                .routeCode("METRO-001")
                .routeName("Metro Line 1")
                .transportType(PredefinedTransportType.METRO)
                .status(PredefinedMasterDataStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 6, 10, 10, 0))
                .build();
    }

    private ImportRoutePreviewResponse samplePreview() {
        return ImportRoutePreviewResponse.builder()
                .totalRows(1)
                .validRows(1)
                .invalidRows(0)
                .items(List.of(ImportRoutePreviewItem.builder()
                        .row(2)
                        .routeName("Metro Line 1")
                        .transportType(PredefinedTransportType.METRO)
                        .valid(true)
                        .errors(List.of())
                        .build()))
                .errors(List.of())
                .build();
    }

    private ImportRouteConfirmResponse sampleConfirm() {
        return ImportRouteConfirmResponse.builder()
                .imported(1)
                .items(List.of(ImportRouteItemResponse.builder()
                        .row(2)
                        .id(10L)
                        .operatorId(1L)
                        .routeCode("METRO-001")
                        .routeName("Metro Line 1")
                        .transportType(PredefinedTransportType.METRO)
                        .status(PredefinedMasterDataStatus.ACTIVE)
                        .build()))
                .build();
    }
}
