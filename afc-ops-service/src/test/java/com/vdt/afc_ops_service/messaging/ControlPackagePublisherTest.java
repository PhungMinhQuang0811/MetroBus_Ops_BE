package com.vdt.afc_ops_service.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControlPackagePublisherTest {

    @Mock
    TopicExchange afcExchange;

    @Mock
    RabbitTemplate rabbitTemplate;

    ControlPackagePublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ControlPackagePublisher(afcExchange, rabbitTemplate);
        when(afcExchange.getName()).thenReturn("afc.exchange");
    }

    @Test
    void publishToStation_publishesToCorrectRoutingKey() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("publishedAt", "2026-06-25T00:00:00Z");
        payload.put("deviceConfig", Map.of("version", 1));

        publisher.publishToStation("METRO-001-ST-001", payload);

        ArgumentCaptor<String> exchangeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> routingKeyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map> payloadCaptor = ArgumentCaptor.forClass(Map.class);

        verify(rabbitTemplate).convertAndSend(exchangeCaptor.capture(), routingKeyCaptor.capture(), payloadCaptor.capture());

        assertEquals("afc.exchange", exchangeCaptor.getValue());
        assertEquals("device.METRO-001-ST-001", routingKeyCaptor.getValue());
        assertEquals("2026-06-25T00:00:00Z", payloadCaptor.getValue().get("publishedAt"));
    }

    @Test
    void publishToStation_failure_doesNotThrow() {
        Map<String, Object> payload = Map.of();
        doThrow(new RuntimeException("RabbitMQ unavailable"))
                .when(rabbitTemplate).convertAndSend("afc.exchange", "device.ST-001", payload);

        // Should not throw
        publisher.publishToStation("ST-001", payload);
    }
}