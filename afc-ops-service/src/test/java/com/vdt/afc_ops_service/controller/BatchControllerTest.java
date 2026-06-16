package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.batch.BatchResponse;
import com.vdt.afc_ops_service.service.IBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BatchControllerTest {

    @Mock
    IBatchService batchService;

    MockMvc mockMvc;
    BatchController batchController;

    @BeforeEach
    void setUp() {
        batchController = new BatchController(batchService);
        mockMvc = MockMvcBuilders.standaloneSetup(batchController).build();
    }

    @Test
    void createBatch_UsesBatchPathAndReturnsCreatedBatch() throws Exception {
        when(batchService.createBatch(any())).thenReturn(BatchResponse.builder()
                .id("BATCH-000001")
                .batchCode("HCMC-METRO-20260604-0001")
                .fromTime(LocalDateTime.of(2026, 6, 4, 0, 0))
                .toTime(LocalDateTime.of(2026, 6, 4, 23, 59, 59))
                .transactionCount(1500)
                .status("CREATED")
                .build());

        mockMvc.perform(post("/batch/create-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fromTime": "2026-06-04T00:00:00",
                                  "toTime": "2026-06-04T23:59:59"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("BATCH-000001"))
                .andExpect(jsonPath("$.result.batchCode").value("HCMC-METRO-20260604-0001"))
                .andExpect(jsonPath("$.result.transactionCount").value(1500))
                .andExpect(jsonPath("$.result.status").value("CREATED"));

        ArgumentCaptor<com.vdt.afc_ops_service.dto.request.batch.CreateBatchRequest> requestCaptor =
                ArgumentCaptor.forClass(com.vdt.afc_ops_service.dto.request.batch.CreateBatchRequest.class);
        verify(batchService).createBatch(requestCaptor.capture());
        assertEquals(LocalDateTime.of(2026, 6, 4, 0, 0), requestCaptor.getValue().getFromTime());
    }

    @Test
    void createBatch_MissingFromTime_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/batch/create-batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toTime": "2026-06-04T23:59:59"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listBatches_UsesBatchPathAndQueryParams() throws Exception {
        when(batchService.listBatches(eq("CREATED"), any(), any(), eq(0), eq(10)))
                .thenReturn(PageResponse.<BatchResponse>builder()
                        .items(List.of(BatchResponse.builder()
                                .id("BATCH-000001")
                                .batchCode("HCMC-METRO-20260604-0001")
                                .status("CREATED")
                                .transactionCount(1500)
                                .build()))
                        .page(0)
                        .size(10)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/batch/list-batches")
                        .param("status", "CREATED")
                        .param("from", "2026-06-04T00:00:00")
                        .param("to", "2026-06-04T23:59:59")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].id").value("BATCH-000001"))
                .andExpect(jsonPath("$.result.items[0].batchCode").value("HCMC-METRO-20260604-0001"))
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }
}
