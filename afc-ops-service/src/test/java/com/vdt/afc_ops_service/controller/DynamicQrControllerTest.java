package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.request.qr.GenerateDynamicQrRequest;
import com.vdt.afc_ops_service.dto.response.qr.DynamicQrResponse;
import com.vdt.afc_ops_service.service.IDynamicQrService;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DynamicQrControllerTest {

    @Mock
    IDynamicQrService dynamicQrService;

    MockMvc mockMvc;
    DynamicQrController dynamicQrController;

    @BeforeEach
    void setUp() {
        dynamicQrController = new DynamicQrController(dynamicQrService);
        mockMvc = MockMvcBuilders.standaloneSetup(dynamicQrController).build();
    }

    @Test
    void generateDynamicQr_UsesRootPath() throws Exception {
        when(dynamicQrService.generate(any())).thenReturn(DynamicQrResponse.builder()
                .qrId("QR-SESSION-000001")
                .qrPayload("AFCQR:v1:QR-SESSION-000001")
                .expiresAt(LocalDateTime.of(2026, 6, 13, 16, 30, 30))
                .refreshAfterSeconds(30)
                .build());

        mockMvc.perform(post("/generate-dynamic-qr")
                        .header("X-External-User-Id", "APP-USER-000001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketId":"TICKET-000001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.qrId").value("QR-SESSION-000001"))
                .andExpect(jsonPath("$.result.qrPayload").value("AFCQR:v1:QR-SESSION-000001"))
                .andExpect(jsonPath("$.result.refreshAfterSeconds").value(30));

        ArgumentCaptor<GenerateDynamicQrRequest> requestCaptor =
                ArgumentCaptor.forClass(GenerateDynamicQrRequest.class);
        verify(dynamicQrService).generate(requestCaptor.capture());
        assertEquals("TICKET-000001", requestCaptor.getValue().getTicketId());
    }

    @Test
    void generateDynamicQr_MissingCardId_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/generate-dynamic-qr")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {}
                                """))
                .andExpect(status().isBadRequest());
    }
}
