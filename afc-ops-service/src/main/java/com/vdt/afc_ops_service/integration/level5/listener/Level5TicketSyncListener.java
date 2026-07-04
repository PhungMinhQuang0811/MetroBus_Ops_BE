package com.vdt.afc_ops_service.integration.level5.listener;

import com.vdt.afc_ops_service.service.IIntegrationExchangeLogService;

import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.ticket.C5TicketUnlinkedMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5TicketSyncService;
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
public class Level5TicketSyncListener {

    static final String TICKET_CREATED = "ticket.created";
    static final String TICKET_UNLINKED = "ticket.unlinked";
    static final String SYNC_TICKET_ALL = "sync.ticket.all";

    ILevel5TicketSyncService level5TicketSyncService;
    ObjectMapper objectMapper;
    IIntegrationExchangeLogService integrationExchangeLogService;

    @RabbitListener(queues = "#{level5TicketSyncProperties.queue()}")
    public void receiveTicketSync(Message message) {
        String payloadString = new String(message.getBody());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            Level5BusinessSyncItemResult result = null;
            Object parsedMsg = null;
            switch (routingKey) {
                case TICKET_CREATED -> {
                    parsedMsg = objectMapper.readValue(payloadString, C5TicketMessage.class);
                    result = level5TicketSyncService.processTicket((C5TicketMessage) parsedMsg);
                }
                case TICKET_UNLINKED -> {
                    parsedMsg = objectMapper.readValue(payloadString, C5TicketUnlinkedMessage.class);
                    result = level5TicketSyncService.processTicketUnlinked((C5TicketUnlinkedMessage) parsedMsg);
                }
                case SYNC_TICKET_ALL -> {
                    parsedMsg = objectMapper.readValue(payloadString, C5TicketSyncMessage.class);
                    result = level5TicketSyncService.processTicketSnapshot((C5TicketSyncMessage) parsedMsg);
                }
                default -> {
                    log.warn("Ignored unsupported Level 5 ticket routing key: {}", routingKey);
                }
            };
            if (result != null) {
                logResult(routingKey, result);
                integrationExchangeLogService.logExchange("Level5", "INBOUND", routingKey, "SUCCESS", parsedMsg, result, null);
            }
        } catch (Exception e) {
            log.error("Failed to process Level 5 ticket sync message: {}", e.getMessage(), e);
            integrationExchangeLogService.logExchange("Level5", "INBOUND", routingKey, "FAILED", payloadString, null, e.getMessage());
        }
    }

    // Removed readPayload
    private void logResult(String routingKey, Level5BusinessSyncItemResult result) {
        if (result != null) {
            log.info("Processed Level 5 ticket sync. routingKey={}, externalId={}, result={}, errorCode={}",
                    routingKey, result.getExternalId(), result.getResult(), result.getErrorCode());
        }
    }
}
