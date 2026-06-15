package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;
import com.vdt.afc_ops_service.service.ITransactionService;
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
class TransactionControllerTest {

    @Mock
    ITransactionService transactionService;

    MockMvc mockMvc;
    TransactionController transactionController;

    @BeforeEach
    void setUp() {
        transactionController = new TransactionController(transactionService);
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController).build();
    }

    @Test
    void submitTransaction_UsesRootPath() throws Exception {
        when(transactionService.submit(any())).thenReturn(SubmitTransactionResponse.builder()
                .transactionId("TXN-000001")
                .decision("OPEN_GATE")
                .reason("VALID")
                .serverTime(LocalDateTime.of(2026, 6, 13, 17, 30))
                .build());

        mockMvc.perform(post("/submit-tap-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceCode": "GATE-001",
                                  "deviceSecret": "secret",
                                  "qrPayload": "AFCQR:v1:QR-000001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.transactionId").value("TXN-000001"))
                .andExpect(jsonPath("$.result.decision").value("OPEN_GATE"))
                .andExpect(jsonPath("$.result.reason").value("VALID"));

        ArgumentCaptor<com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest> requestCaptor =
                ArgumentCaptor.forClass(com.vdt.afc_ops_service.dto.request.transaction.SubmitTransactionRequest.class);
        verify(transactionService).submit(requestCaptor.capture());
        assertEquals("GATE-001", requestCaptor.getValue().getDeviceCode());
    }

    @Test
    void submitTransaction_MissingQrPayload_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/submit-tap-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceCode": "GATE-001",
                                  "deviceSecret": "secret"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
