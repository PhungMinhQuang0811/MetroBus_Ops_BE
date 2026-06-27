package com.vdt.afc_ops_service.integration.level5.listener;

import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5TicketSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Level5TicketSyncListenerTest {

    @Mock
    ILevel5TicketSyncService level5TicketSyncService;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    Level5TicketSyncListener listener;

    @Test
    void receiveTicketSync_ticketCreated_callsProcessTicket() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("ticket.created");
        C5TicketMessage msg = C5TicketMessage.builder().ticketId(UUID.randomUUID()).build();

        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("CREATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5TicketMessage.class))).thenReturn(msg);
        when(level5TicketSyncService.processTicket(any(C5TicketMessage.class))).thenReturn(result);

        listener.receiveTicketSync(new Message("{}".getBytes(), props));

        verify(level5TicketSyncService).processTicket(any(C5TicketMessage.class));
    }

    @Test
    void receiveTicketSync_ticketUnlinked_callsProcessTicketUnlinked() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("ticket.unlinked");
        C5TicketUnlinkedMessage msg = new C5TicketUnlinkedMessage();
        msg.setTicketId(UUID.randomUUID());

        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("UPDATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5TicketUnlinkedMessage.class))).thenReturn(msg);
        when(level5TicketSyncService.processTicketUnlinked(any(C5TicketUnlinkedMessage.class))).thenReturn(result);

        listener.receiveTicketSync(new Message("{}".getBytes(), props));

        verify(level5TicketSyncService).processTicketUnlinked(any(C5TicketUnlinkedMessage.class));
    }

    @Test
    void receiveTicketSync_syncTicketAll_callsProcessTicketSnapshot() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("sync.ticket.all");
        C5TicketSyncMessage msg = C5TicketSyncMessage.builder().id(UUID.randomUUID()).build();

        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("CREATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5TicketSyncMessage.class))).thenReturn(msg);
        when(level5TicketSyncService.processTicketSnapshot(any(C5TicketSyncMessage.class))).thenReturn(result);

        listener.receiveTicketSync(new Message("{}".getBytes(), props));

        verify(level5TicketSyncService).processTicketSnapshot(any(C5TicketSyncMessage.class));
    }

    @Test
    void receiveTicketSync_unsupportedRoutingKey_ignoresAndLogsWarning() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("unsupported.key");

        listener.receiveTicketSync(new Message("{}".getBytes(), props));

        verify(level5TicketSyncService, never()).processTicket(any());
        verify(level5TicketSyncService, never()).processTicketUnlinked(any());
        verify(level5TicketSyncService, never()).processTicketSnapshot(any());
    }
}
