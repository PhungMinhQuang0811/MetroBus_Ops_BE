package com.vdt.afc_ops_service.messaging;

import com.vdt.afc_ops_service.messaging.dto.RouteSyncMessage;
import com.vdt.afc_ops_service.messaging.dto.StationSyncMessage;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class StationRouteSyncPublisher {

    RabbitTemplate rabbitTemplate;

    static final String AFC_EXCHANGE = "afc.exchange";
    static final String ROUTE_SYNCED_ROUTING_KEY = "route.synced";
    static final String STATION_SYNCED_ROUTING_KEY = "station.synced";

    public void publishRouteSync(RouteSyncMessage message) {
        try {
            rabbitTemplate.convertAndSend(AFC_EXCHANGE, ROUTE_SYNCED_ROUTING_KEY, message);
            log.info("Published route sync message: {}", message.getRouteCode());
        } catch (Exception e) {
            log.error("Failed to publish route sync message: {}", message.getRouteCode(), e);
        }
    }

    public void publishStationSync(StationSyncMessage message) {
        try {
            log.info("Publishing station sync: stationCode={}, stationName={}, stationOrder={}, routeCode={}, status={}, distance={}",
                    message.getStationCode(), message.getStationName(), message.getStationOrder(),
                    message.getRouteCode(), message.getStatus(), message.getDistance());
            rabbitTemplate.convertAndSend(AFC_EXCHANGE, STATION_SYNCED_ROUTING_KEY, message);
            log.info("Published station sync successfully: {}", message.getStationCode());
        } catch (Exception e) {
            log.error("Failed to publish station sync: stationCode={}, distance={}", message.getStationCode(), message.getDistance(), e);
        }
    }
}
