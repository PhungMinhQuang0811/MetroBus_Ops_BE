package com.vdt.afc_ops_service.integration.level5.listener;

import com.vdt.afc_ops_service.integration.level5.dto.message.entitlement.C5EntitlementMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.entitlement.C5EntitlementUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5EntitlementSyncService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class Level5EntitlementSyncListener {

    static final String TICKET_CREATED = "ticket.created";
    static final String TICKET_UNLINKED = "ticket.unlinked";
    static final String SYNC_TICKET_ALL = "sync.ticket.all";

    ILevel5EntitlementSyncService level5EntitlementSyncService;
    ObjectMapper objectMapper;

    @RabbitListener(queues = "#{level5EntitlementSyncProperties.queue()}")
    public void receiveEntitlementSync(Message message) throws IOException {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        Level5BusinessSyncItemResult result = switch (routingKey) {
            case TICKET_CREATED -> level5EntitlementSyncService.processEntitlement(
                    readPayload(message, C5EntitlementMessage.class)
            );
            case TICKET_UNLINKED -> level5EntitlementSyncService.processTicketUnlinked(
                    readPayload(message, C5EntitlementUnlinkedMessage.class)
            );
            case SYNC_TICKET_ALL -> level5EntitlementSyncService.processEntitlementSnapshot(
                    readPayload(message, C5TicketSyncMessage.class)
            );
            default -> {
                log.warn("Ignored unsupported Level 5 entitlement routing key: {}", routingKey);
                yield null;
            }
        };
        logResult(routingKey, result);
    }

    private <T> T readPayload(Message message, Class<T> payloadType) throws IOException {
        return objectMapper.readValue(message.getBody(), payloadType);
    }

    private void logResult(String routingKey, Level5BusinessSyncItemResult result) {
        if (result != null) {
            log.info("Processed Level 5 entitlement sync. routingKey={}, externalId={}, result={}, errorCode={}",
                    routingKey, result.getExternalId(), result.getResult(), result.getErrorCode());
        }
    }
}
