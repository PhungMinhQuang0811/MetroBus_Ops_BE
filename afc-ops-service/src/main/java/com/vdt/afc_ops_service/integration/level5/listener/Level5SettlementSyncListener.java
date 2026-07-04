package com.vdt.afc_ops_service.integration.level5.listener;

import com.vdt.afc_ops_service.service.IIntegrationExchangeLogService;

import com.vdt.afc_ops_service.integration.level5.dto.message.settlement.CompanyShareMessage;
import com.vdt.afc_ops_service.integration.level5.dto.message.settlement.SettlementConfirmedEvent;
import com.vdt.afc_ops_service.service.ISettlementService;
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
public class Level5SettlementSyncListener {

    ISettlementService settlementService;
    ObjectMapper objectMapper;
    IIntegrationExchangeLogService integrationExchangeLogService;

    @RabbitListener(queues = "#{level5SettlementSyncProperties.queue()}")
    public void receiveSettlementSync(Message message) {
        String payloadString = new String(message.getBody());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            SettlementConfirmedEvent event = objectMapper.readValue(payloadString, SettlementConfirmedEvent.class);

            if (event.shares() == null || event.shares().isEmpty()) {
                log.warn("Received settlement event with no shares. settlementId={}", event.settlementId());
                integrationExchangeLogService.logExchange("Level5", "INBOUND", routingKey, "SUCCESS", event, null, "No shares found");
                return;
            }

            for (CompanyShareMessage share : event.shares()) {
                try {
                    settlementService.processSettlement(event.settlementId(), event.period(), share);
                    log.info("Processed settlement: settlementId={}, period={}, operatorCode={}",
                            event.settlementId(), event.period(), share.operatorCode());
                } catch (Exception exception) {
                    log.error("Failed to process settlement share: settlementId={}, operatorCode={}",
                            event.settlementId(), share.operatorCode(), exception);
                }
            }
            integrationExchangeLogService.logExchange("Level5", "INBOUND", routingKey, "SUCCESS", event, null, null);
        } catch (Exception e) {
            log.error("Failed to process settlement sync message: {}", e.getMessage(), e);
            integrationExchangeLogService.logExchange("Level5", "INBOUND", routingKey, "FAILED", payloadString, null, e.getMessage());
        }
    }
}