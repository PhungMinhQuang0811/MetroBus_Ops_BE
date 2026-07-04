package com.vdt.afc_ops_service.integration.level5.listener;

import com.vdt.afc_ops_service.service.IIntegrationExchangeLogService;

import com.vdt.afc_ops_service.integration.level5.dto.message.operator.C5OperatorEventMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.operator.C5OperatorSyncMessage;
import com.vdt.afc_ops_service.integration.level5.dto.response.Level5BusinessSyncItemResult;
import com.vdt.afc_ops_service.integration.level5.service.ILevel5OperatorSyncService;
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
public class Level5OperatorSyncListener {

    static final String SYNC_OPERATOR_ALL = "sync.operator.all";
    static final String OPERATOR_CREATED  = "operator.created";
    static final String OPERATOR_UPDATED  = "operator.updated";

    ILevel5OperatorSyncService level5OperatorSyncService;
    ObjectMapper objectMapper;
    IIntegrationExchangeLogService integrationExchangeLogService;

    @RabbitListener(queues = "#{level5OperatorSyncProperties.queue()}")
    public void receiveOperatorSync(Message message) {
        String payloadString = new String(message.getBody());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            Level5BusinessSyncItemResult result = null;
            Object parsedMsg = null;
            switch (routingKey) {
                case SYNC_OPERATOR_ALL -> {
                    parsedMsg = objectMapper.readValue(payloadString, C5OperatorSyncMessage.class);
                    result = level5OperatorSyncService.processOperatorSnapshot((C5OperatorSyncMessage) parsedMsg);
                }
                case OPERATOR_CREATED, OPERATOR_UPDATED -> {
                    parsedMsg = objectMapper.readValue(payloadString, C5OperatorEventMessage.class);
                    C5OperatorEventMessage event = (C5OperatorEventMessage) parsedMsg;
                    result = level5OperatorSyncService.processOperatorSnapshot(
                            C5OperatorSyncMessage.builder()
                                    .id(event.getOperatorId())
                                    .code(event.getCode())
                                    .name(event.getName())
                                    .status(event.getStatus())
                                    .build()
                    );
                }
                default -> {
                    log.warn("Ignored unsupported Level 5 operator routing key: {}", routingKey);
                }
            };
            if (result != null) {
                logResult(routingKey, result);
                integrationExchangeLogService.logExchange("Level5", "INBOUND", routingKey, "SUCCESS", parsedMsg, result, null);
            }
        } catch (Exception e) {
            log.error("Failed to process Level 5 operator sync message: {}", e.getMessage(), e);
            integrationExchangeLogService.logExchange("Level5", "INBOUND", routingKey, "FAILED", payloadString, null, e.getMessage());
        }
    }

    // Removed readPayload
    private void logResult(String routingKey, Level5BusinessSyncItemResult result) {
        if (result != null) {
            log.info("Processed Level 5 operator sync. routingKey={}, externalId={}, result={}, errorCode={}",
                    routingKey, result.getExternalId(), result.getResult(), result.getErrorCode());
        }
    }
}
