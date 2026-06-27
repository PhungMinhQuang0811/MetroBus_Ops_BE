package com.vdt.afc_ops_service.integration.level5.listener;

import com.vdt.afc_ops_service.integration.level5.dto.message.operator.C5OperatorEventMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.operator.C5OperatorSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5OperatorSyncService;
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
class Level5OperatorSyncListenerTest {

    @Mock
    ILevel5OperatorSyncService level5OperatorSyncService;

    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    Level5OperatorSyncListener listener;

    @Test
    void receiveOperatorSync_syncOperatorAll_callsProcessOperatorSnapshot() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("sync.operator.all");
        C5OperatorSyncMessage msg = C5OperatorSyncMessage.builder()
                .id(UUID.randomUUID())
                .code("OP1")
                .name("Operator 1")
                .status("ACTIVE")
                .build();

        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("CREATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5OperatorSyncMessage.class))).thenReturn(msg);
        when(level5OperatorSyncService.processOperatorSnapshot(any(C5OperatorSyncMessage.class))).thenReturn(result);

        listener.receiveOperatorSync(new Message("{}".getBytes(), props));

        verify(level5OperatorSyncService).processOperatorSnapshot(any(C5OperatorSyncMessage.class));
    }

    @Test
    void receiveOperatorSync_operatorCreated_callsProcessOperatorSnapshotWithMappedEvent() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("operator.created");
        C5OperatorEventMessage event = new C5OperatorEventMessage();
        event.setOperatorId(UUID.randomUUID());
        event.setCode("OP2");
        event.setName("Operator 2");
        event.setStatus("ACTIVE");

        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("CREATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5OperatorEventMessage.class))).thenReturn(event);
        when(level5OperatorSyncService.processOperatorSnapshot(any(C5OperatorSyncMessage.class))).thenReturn(result);

        listener.receiveOperatorSync(new Message("{}".getBytes(), props));

        verify(level5OperatorSyncService).processOperatorSnapshot(org.mockito.Mockito.argThat(msg -> 
                event.getOperatorId().equals(msg.getId()) &&
                "OP2".equals(msg.getCode()) &&
                "Operator 2".equals(msg.getName()) &&
                "ACTIVE".equals(msg.getStatus())
        ));
    }

    @Test
    void receiveOperatorSync_operatorUpdated_callsProcessOperatorSnapshotWithMappedEvent() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("operator.updated");
        C5OperatorEventMessage event = new C5OperatorEventMessage();
        event.setOperatorId(UUID.randomUUID());
        event.setCode("OP2");
        event.setName("Operator 2 Updated");
        event.setStatus("DISABLED");

        Level5BusinessSyncItemResult result = Level5BusinessSyncItemResult.builder().externalId("ext-1").result("UPDATED").build();
        when(objectMapper.readValue(any(byte[].class), eq(C5OperatorEventMessage.class))).thenReturn(event);
        when(level5OperatorSyncService.processOperatorSnapshot(any(C5OperatorSyncMessage.class))).thenReturn(result);

        listener.receiveOperatorSync(new Message("{}".getBytes(), props));

        verify(level5OperatorSyncService).processOperatorSnapshot(org.mockito.Mockito.argThat(msg -> 
                event.getOperatorId().equals(msg.getId()) &&
                "OP2".equals(msg.getCode()) &&
                "Operator 2 Updated".equals(msg.getName()) &&
                "DISABLED".equals(msg.getStatus())
        ));
    }

    @Test
    void receiveOperatorSync_unsupportedRoutingKey_ignoresAndLogsWarning() throws IOException {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey("unsupported.key");

        listener.receiveOperatorSync(new Message("{}".getBytes(), props));

        verify(level5OperatorSyncService, never()).processOperatorSnapshot(any());
    }
}
