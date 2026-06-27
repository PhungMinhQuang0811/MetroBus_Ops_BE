package com.vdt.afc_ops_service.integration.level5.listener;

import com.vdt.afc_ops_service.integration.level5.dto.message.settlement.CompanyShareMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.settlement.SettlementConfirmedEvent;
import com.vdt.afc_ops_service.service.ISettlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Level5SettlementSyncListenerTest {

    @Mock
    ISettlementService settlementService;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    Level5SettlementSyncListener listener;

    @Test
    void receiveSettlementSync_emptyShares_logsWarningAndSkipsProcessing() throws IOException {
        SettlementConfirmedEvent event = new SettlementConfirmedEvent(
                UUID.randomUUID(), "2026-06", null
        );

        when(objectMapper.readValue(any(byte[].class), eq(SettlementConfirmedEvent.class))).thenReturn(event);

        listener.receiveSettlementSync(new Message("{}".getBytes(), new MessageProperties()));

        verify(settlementService, never()).processSettlement(any(), any(), any());
    }

    @Test
    void receiveSettlementSync_validShares_processesEachShare() throws IOException {
        UUID settlementId = UUID.randomUUID();
        CompanyShareMessage share1 = new CompanyShareMessage(UUID.randomUUID(), "OP1", BigDecimal.TEN, BigDecimal.ONE, 5, BigDecimal.ONE);
        CompanyShareMessage share2 = new CompanyShareMessage(UUID.randomUUID(), "OP2", BigDecimal.valueOf(20), BigDecimal.valueOf(2), 10, BigDecimal.valueOf(2));
        SettlementConfirmedEvent event = new SettlementConfirmedEvent(
                settlementId, "2026-06", List.of(share1, share2)
        );

        when(objectMapper.readValue(any(byte[].class), eq(SettlementConfirmedEvent.class))).thenReturn(event);

        listener.receiveSettlementSync(new Message("{}".getBytes(), new MessageProperties()));

        verify(settlementService).processSettlement(eq(settlementId), eq("2026-06"), eq(share1));
        verify(settlementService).processSettlement(eq(settlementId), eq("2026-06"), eq(share2));
    }

    @Test
    void receiveSettlementSync_processThrowsException_logsErrorAndContinues() throws IOException {
        UUID settlementId = UUID.randomUUID();
        CompanyShareMessage share1 = new CompanyShareMessage(UUID.randomUUID(), "OP1", BigDecimal.TEN, BigDecimal.ONE, 5, BigDecimal.ONE);
        CompanyShareMessage share2 = new CompanyShareMessage(UUID.randomUUID(), "OP2", BigDecimal.valueOf(20), BigDecimal.valueOf(2), 10, BigDecimal.valueOf(2));
        SettlementConfirmedEvent event = new SettlementConfirmedEvent(
                settlementId, "2026-06", List.of(share1, share2)
        );

        when(objectMapper.readValue(any(byte[].class), eq(SettlementConfirmedEvent.class))).thenReturn(event);
        doThrow(new RuntimeException("DB error"))
                .when(settlementService).processSettlement(eq(settlementId), eq("2026-06"), eq(share1));

        listener.receiveSettlementSync(new Message("{}".getBytes(), new MessageProperties()));

        // Verify it tried OP1 and threw exception
        verify(settlementService).processSettlement(eq(settlementId), eq("2026-06"), eq(share1));
        // Verify it still continued and processed OP2
        verify(settlementService).processSettlement(eq(settlementId), eq("2026-06"), eq(share2));
    }
}
