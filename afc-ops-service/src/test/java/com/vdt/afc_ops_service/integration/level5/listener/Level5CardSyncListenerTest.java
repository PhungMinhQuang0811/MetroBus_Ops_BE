package com.vdt.afc_ops_service.integration.level5.listener;

import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5BlacklistMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardStatusMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5CardSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Level5CardSyncListenerTest {

    @Mock
    ILevel5CardSyncService level5CardSyncService;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    Level5CardSyncListener listener;

    @Test
    void receiveCardSync_blacklistAdded_callsProcessBlacklist() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("blacklist.added");
        C5BlacklistMessage msg = new C5BlacklistMessage(
                UUID.randomUUID(), UUID.randomUUID(), "ADDED", "LOST_CARD", UUID.randomUUID(), Instant.now());
        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("CREATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5BlacklistMessage.class))).thenReturn(msg);
        when(level5CardSyncService.processBlacklist(eq("blacklist.added"), any(C5BlacklistMessage.class))).thenReturn(result);

        listener.receiveCardSync(new Message("{}".getBytes(), props));

        verify(level5CardSyncService).processBlacklist(eq("blacklist.added"), any(C5BlacklistMessage.class));
    }

    @Test
    void receiveCardSync_blacklistRemoved_callsProcessBlacklist() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("blacklist.removed");
        C5BlacklistMessage msg = new C5BlacklistMessage(
                UUID.randomUUID(), UUID.randomUUID(), "REMOVED", null, UUID.randomUUID(), Instant.now());
        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("UPDATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5BlacklistMessage.class))).thenReturn(msg);
        when(level5CardSyncService.processBlacklist(eq("blacklist.removed"), any(C5BlacklistMessage.class))).thenReturn(result);

        listener.receiveCardSync(new Message("{}".getBytes(), props));

        verify(level5CardSyncService).processBlacklist(eq("blacklist.removed"), any(C5BlacklistMessage.class));
    }

    @Test
    void receiveCardSync_blacklistAll_callsProcessBlacklistSnapshot() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("sync.blacklist.all");
        C5BlacklistMessage msg = new C5BlacklistMessage(
                UUID.randomUUID(), UUID.randomUUID(), null, "LOST_CARD", UUID.randomUUID(), Instant.now());
        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("CREATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5BlacklistMessage.class))).thenReturn(msg);
        when(level5CardSyncService.processBlacklistSnapshot(any(C5BlacklistMessage.class))).thenReturn(result);

        listener.receiveCardSync(new Message("{}".getBytes(), props));

        verify(level5CardSyncService).processBlacklistSnapshot(any(C5BlacklistMessage.class));
    }

    @Test
    void receiveCardSync_cardStatusChanged_callsProcessCardStatus() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("card.status.changed");
        C5CardStatusMessage msg = new C5CardStatusMessage();
        msg.setCardId(UUID.randomUUID());
        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("UPDATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5CardStatusMessage.class))).thenReturn(msg);
        when(level5CardSyncService.processCardStatus(any(C5CardStatusMessage.class))).thenReturn(result);

        listener.receiveCardSync(new Message("{}".getBytes(), props));

        verify(level5CardSyncService).processCardStatus(any(C5CardStatusMessage.class));
    }

    @Test
    void receiveCardSync_cardAll_callsProcessCardSnapshot() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("sync.card.all");
        C5CardSyncMessage msg = new C5CardSyncMessage();
        msg.setId(UUID.randomUUID());
        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("CREATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5CardSyncMessage.class))).thenReturn(msg);
        when(level5CardSyncService.processCardSnapshot(any(C5CardSyncMessage.class))).thenReturn(result);

        listener.receiveCardSync(new Message("{}".getBytes(), props));

        verify(level5CardSyncService).processCardSnapshot(any(C5CardSyncMessage.class));
    }

    @Test
    void receiveCardSync_unsupportedRoutingKey_ignoresAndLogsWarning() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("unsupported.key");

        listener.receiveCardSync(new Message("{}".getBytes(), props));

        verify(level5CardSyncService, never()).processCardStatus(any());
        verify(level5CardSyncService, never()).processBlacklist(any(), any());
        verify(level5CardSyncService, never()).processCardSnapshot(any());
        verify(level5CardSyncService, never()).processBlacklistSnapshot(any());
    }
}