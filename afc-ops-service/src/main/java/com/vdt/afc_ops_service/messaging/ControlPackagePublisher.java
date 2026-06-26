package com.vdt.afc_ops_service.messaging;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ControlPackagePublisher {

    TopicExchange afcExchange;
    RabbitTemplate rabbitTemplate;

    public void publishToStation(String stationCode, Map<String, Object> combinedPayload) {
        String routingKey = "device." + stationCode;
        try {
            rabbitTemplate.convertAndSend(afcExchange.getName(), routingKey, combinedPayload);
            log.info("Published combined control package to station {} via routing key {}", stationCode, routingKey);
        } catch (Exception e) {
            log.error("Failed to publish control package to station {}: {}", stationCode, e.getMessage(), e);
        }
    }
}