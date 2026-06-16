package com.vdt.afc_ops_service.repository.mongo;

import com.vdt.afc_ops_service.document.DeviceIncident;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeviceIncidentRepository extends MongoRepository<DeviceIncident, String> {
    Optional<DeviceIncident> findFirstByDeviceIdOrderByOccurredAtDesc(Long deviceId);
}
