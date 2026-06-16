package com.vdt.afc_ops_service.repository.mongo;

import com.vdt.afc_ops_service.document.DeviceHeartbeat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeviceHeartbeatRepository extends MongoRepository<DeviceHeartbeat, String> {
}
