package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.controlpackage.AckControlPackageApplyRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.CreateControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.PublishControlPackageRequest;
import com.vdt.afc_ops_service.dto.request.controlpackage.UpdateControlPackageRequest;
import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageAckResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackagePublishResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncDetailResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.ControlPackageSyncResponse;
import com.vdt.afc_ops_service.dto.response.controlpackage.PendingControlPackageResponse;
import com.vdt.afc_ops_service.service.IControlPackageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AfcOpsControlPackageControllerTest {

    @Mock
    IControlPackageService controlPackageService;

    MockMvc mockMvc;
    AfcOpsControlPackageController controller;

    @BeforeEach
    void setUp() {
        controller = new AfcOpsControlPackageController(controlPackageService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void create_ReturnsSavedPackage() throws Exception {
        when(controlPackageService.create(any(CreateControlPackageRequest.class)))
                .thenReturn(ControlPackageResponse.builder()
                        .id(101L)
                        .version(13L)
                        .packageType("DEVICE_CONFIG")
                        .sourceType("LEVEL4_CREATED")
                        .status("CREATED")
                        .build());

        mockMvc.perform(post("/control-package/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "packageType": "DEVICE_CONFIG",
                                  "payload": {
                                    "maxOfflineSeconds": 60
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(101))
                .andExpect(jsonPath("$.result.version").value(13))
                .andExpect(jsonPath("$.result.status").value("CREATED"));

        verify(controlPackageService).create(any(CreateControlPackageRequest.class));
    }

    @Test
    void update_ReturnsUpdatedPackage() throws Exception {
        when(controlPackageService.update(eq(101L), any(UpdateControlPackageRequest.class)))
                .thenReturn(ControlPackageResponse.builder()
                        .id(101L)
                        .version(13L)
                        .packageType("DEVICE_CONFIG")
                        .sourceType("LEVEL4_CREATED")
                        .status("CREATED")
                        .build());

        mockMvc.perform(post("/control-package/update/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payload": {
                                    "maxOfflineSeconds": 90
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(101))
                .andExpect(jsonPath("$.result.status").value("CREATED"));

        verify(controlPackageService).update(eq(101L), any(UpdateControlPackageRequest.class));
    }

    @Test
    void getDetail_ReturnsFullPackageDetail() throws Exception {
        when(controlPackageService.getDetail(101L))
                .thenReturn(ControlPackageDetailResponse.builder()
                        .id(101L)
                        .version(13L)
                        .packageType("DEVICE_CONFIG")
                        .sourceType("LEVEL4_CREATED")
                        .status("CREATED")
                        .payload(Map.of("maxOfflineSeconds", 60))
                        .build());

        mockMvc.perform(get("/control-package/get-detail")
                        .param("packageId", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(101))
                .andExpect(jsonPath("$.result.payload.maxOfflineSeconds").value(60));

        verify(controlPackageService).getDetail(101L);
    }

    @Test
    void list_ReturnsPaginatedList() throws Exception {
        when(controlPackageService.list("DEVICE_CONFIG", null, "CREATED", 0, 20))
                .thenReturn(PageResponse.<ControlPackageResponse>builder()
                        .items(List.of(ControlPackageResponse.builder()
                                .id(101L)
                                .version(13L)
                                .build()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/control-package/list")
                        .param("packageType", "DEVICE_CONFIG")
                        .param("status", "CREATED")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].id").value(101))
                .andExpect(jsonPath("$.result.totalElements").value(1));

        verify(controlPackageService).list("DEVICE_CONFIG", null, "CREATED", 0, 20);
    }

    @Test
    void publish_ReturnsPublishResults() throws Exception {
        when(controlPackageService.publish(eq(101L), any(PublishControlPackageRequest.class)))
                .thenReturn(ControlPackagePublishResponse.builder()
                        .packageId(101L)
                        .status("PUBLISHED")
                        .build());

        mockMvc.perform(post("/control-package/publish/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationIds": [1, 2]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.packageId").value(101))
                .andExpect(jsonPath("$.result.status").value("PUBLISHED"));

        verify(controlPackageService).publish(eq(101L), any(PublishControlPackageRequest.class));
    }

    @Test
    void pullPending_ReturnsSyncPayloads() throws Exception {
        when(controlPackageService.pullPending("ST-BT", 12L))
                .thenReturn(List.of(PendingControlPackageResponse.builder()
                        .syncId(500L)
                        .packageId(101L)
                        .version(13L)
                        .payload(Map.of("key", "val"))
                        .build()));

        mockMvc.perform(get("/control-package/pull-pending")
                        .param("stationCode", "ST-BT")
                        .param("currentVersion", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].syncId").value(500))
                .andExpect(jsonPath("$.result[0].packageId").value(101));

        verify(controlPackageService).pullPending("ST-BT", 12L);
    }

    @Test
    void ackApply_ReturnsAckStatus() throws Exception {
        when(controlPackageService.ackApply(eq(500L), any(AckControlPackageApplyRequest.class)))
                .thenReturn(ControlPackageAckResponse.builder()
                        .syncId(500L)
                        .syncStatus("APPLIED")
                        .build());

        mockMvc.perform(post("/control-package/ack-apply/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "syncStatus": "APPLIED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.syncId").value(500))
                .andExpect(jsonPath("$.result.syncStatus").value("APPLIED"));

        verify(controlPackageService).ackApply(eq(500L), any(AckControlPackageApplyRequest.class));
    }

    @Test
    void searchSyncs_ReturnsPaginatedSyncs() throws Exception {
        when(controlPackageService.searchSyncs("DEVICE_CONFIG", 13L, null, null, 0, 20))
                .thenReturn(PageResponse.<ControlPackageSyncResponse>builder()
                        .items(List.of(ControlPackageSyncResponse.builder()
                                .syncId(500L)
                                .stationName("Bến Thành")
                                .build()))
                        .page(0)
                        .size(20)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/control-package/search-syncs")
                        .param("packageType", "DEVICE_CONFIG")
                        .param("version", "13")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].syncId").value(500))
                .andExpect(jsonPath("$.result.totalElements").value(1));

        verify(controlPackageService).searchSyncs("DEVICE_CONFIG", 13L, null, null, 0, 20);
    }

    @Test
    void getSyncDetail_ReturnsSyncDetail() throws Exception {
        when(controlPackageService.getSyncDetail(500L))
                .thenReturn(ControlPackageSyncDetailResponse.builder()
                        .syncId(500L)
                        .stationName("Bến Thành")
                        .routeName("Line 1")
                        .errorMessage("Mongo error")
                        .build());

        mockMvc.perform(get("/control-package/get-sync-detail")
                        .param("syncId", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.syncId").value(500))
                .andExpect(jsonPath("$.result.errorMessage").value("Mongo error"));

        verify(controlPackageService).getSyncDetail(500L);
    }
}
