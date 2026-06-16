package com.vdt.afc_ops_service.controller;

import com.vdt.afc_ops_service.dto.response.PageResponse;
import com.vdt.afc_ops_service.dto.response.transaction.SubmitTransactionResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionDetailResponse;
import com.vdt.afc_ops_service.dto.response.transaction.TransactionListItemResponse;
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

        mockMvc.perform(post("/transaction/submit-tap-event")
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
        mockMvc.perform(post("/transaction/submit-tap-event")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceCode": "GATE-001",
                                  "deviceSecret": "secret"
                                }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchTransactions_UsesRootPathAndQueryParams() throws Exception {
        when(transactionService.searchTransactions(any(), any(), eq(1L), eq(2L), eq(3L),
                eq("CARD-000001"), eq("TICKET-000001"), eq("ENT-000001"), eq("TAP_IN"),
                eq("OPEN_GATE"), eq("VALID"), eq("PENDING"), eq("CONFIRMED"), eq(0), eq(10)))
                .thenReturn(PageResponse.<TransactionListItemResponse>builder()
                        .items(List.of(TransactionListItemResponse.builder()
                                .id("TX-000001")
                                .eventId("EVT-000001")
                                .stationCode("ST-001")
                                .deviceCode("GATE-001")
                                .tapType("TAP_IN")
                                .decision("OPEN_GATE")
                                .reason("VALID")
                                .syncStatus("PENDING")
                                .build()))
                        .page(0)
                        .size(10)
                        .totalElements(1)
                        .totalPages(1)
                        .build());

        mockMvc.perform(get("/transaction/search-transactions")
                        .param("from", "2026-06-15T00:00:00")
                        .param("to", "2026-06-15T23:59:00")
                        .param("routeId", "1")
                        .param("stationId", "2")
                        .param("deviceId", "3")
                        .param("cardId", "CARD-000001")
                        .param("ticketId", "TICKET-000001")
                        .param("entitlementId", "ENT-000001")
                        .param("tapType", "TAP_IN")
                        .param("decision", "OPEN_GATE")
                        .param("reason", "VALID")
                        .param("syncStatus", "PENDING")
                        .param("ticketProcessingStatus", "CONFIRMED")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.items[0].id").value("TX-000001"))
                .andExpect(jsonPath("$.result.items[0].stationCode").value("ST-001"))
                .andExpect(jsonPath("$.result.totalElements").value(1));
    }

    @Test
    void getTransactionDetail_UsesRootPath() throws Exception {
        when(transactionService.getTransactionDetail("TX-000001"))
                .thenReturn(TransactionDetailResponse.builder()
                        .id("TX-000001")
                        .eventId("EVT-000001")
                        .deviceCode("GATE-001")
                        .deviceDirection("ENTRY")
                        .decision("DENY")
                        .reason("QR_EXPIRED")
                        .rawEventAvailable(false)
                        .build());

        mockMvc.perform(get("/transaction/get-transaction-detail")
                        .param("transactionId", "TX-000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value("TX-000001"))
                .andExpect(jsonPath("$.result.deviceCode").value("GATE-001"))
                .andExpect(jsonPath("$.result.rawEventAvailable").value(false));
    }
}
