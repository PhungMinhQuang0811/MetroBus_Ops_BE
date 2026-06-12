package com.vdt.afc_ops_service.integration.level5.listener;

import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5BlacklistMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.card.C5CardStatusMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5CardSyncService;
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
public class Level5CardSyncListener {

    static final String CARD_STATUS_CHANGED = "card.status.changed";
    static final String BLACKLIST_ADDED = "blacklist.added";
    static final String BLACKLIST_REMOVED = "blacklist.removed";
    static final String SYNC_CARD_ALL = "sync.card.all";

    ILevel5CardSyncService level5CardSyncService;
    ObjectMapper objectMapper;

    @RabbitListener(queues = "#{level5CardSyncProperties.queue()}")
    public void receiveCardSync(Message message) throws IOException {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        Level5BusinessSyncItemResult result = switch (routingKey) {
            case CARD_STATUS_CHANGED -> level5CardSyncService.processCardStatus(
                    readPayload(message, C5CardStatusMessage.class)
            );
            case BLACKLIST_ADDED, BLACKLIST_REMOVED -> level5CardSyncService.processBlacklist(
                    routingKey,
                    readPayload(message, C5BlacklistMessage.class)
            );
            case SYNC_CARD_ALL -> level5CardSyncService.processCardSnapshot(
                    readPayload(message, C5CardSyncMessage.class)
            );
            default -> {
                log.warn("Ignored unsupported Level 5 card routing key: {}", routingKey);
                yield null;
            }
        };
        logResult("card", routingKey, result);
    }

    private <T> T readPayload(Message message, Class<T> payloadType) throws IOException {
        return objectMapper.readValue(message.getBody(), payloadType);
    }

    private void logResult(String stream, String routingKey, Level5BusinessSyncItemResult result) {
        if (result != null) {
            log.info("Processed Level 5 {} sync. routingKey={}, externalId={}, result={}, errorCode={}",
                    stream, routingKey, result.getExternalId(), result.getResult(), result.getErrorCode());
        }
    }
}
