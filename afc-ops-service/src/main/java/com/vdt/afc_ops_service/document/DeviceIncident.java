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

@Document(collection = "device_incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndexes({
    @CompoundIndex(name = "idx_device_occurred", def = "{'device_id': 1, 'occurred_at': -1}"),
    @CompoundIndex(name = "idx_station_occurred", def = "{'station_id': 1, 'occurred_at': -1}"),
    @CompoundIndex(name = "idx_severity_occurred", def = "{'severity': 1, 'occurred_at': -1}"),
    @CompoundIndex(name = "idx_operator_occurred", def = "{'operator_id': 1, 'occurred_at': -1}")
})
public class DeviceIncident {

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

    @Field("incident_type")
    private String incidentType;

    private String severity;

    private String message;

    @Field("occurred_at")
    private LocalDateTime occurredAt;

    @Field("received_at")
    private LocalDateTime receivedAt;

    private Map<String, Object> payload;

    @Field("resolved_at")
    private LocalDateTime resolvedAt;
}
