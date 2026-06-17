package com.vdt.afc_ops_service.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.Map;

@Document(collection = "device_heartbeats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "idx_device_received", def = "{'device_id': 1, 'received_at': -1}"),
    @CompoundIndex(name = "idx_station_received", def = "{'station_id': 1, 'received_at': -1}"),
    @CompoundIndex(name = "idx_operator_received", def = "{'operator_id': 1, 'received_at': -1}")
})
public class DeviceHeartbeat {

    @Id
    private String id;

    @Field("operator_id")
    private Long operatorId;

    @Field("device_id")
    private Long deviceId;

    @Field("device_code")
    private String deviceCode;

    @Field("station_id")
    private Long stationId;

    private String status;

    @Field("firmware_version")
    private String firmwareVersion;

    @Field("sent_at")
    private LocalDateTime sentAt;

    @Field("received_at")
    private LocalDateTime receivedAt;

    private Map<String, Object> payload;
}
