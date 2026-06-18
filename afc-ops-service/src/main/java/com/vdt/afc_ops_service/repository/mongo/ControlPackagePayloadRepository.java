package com.vdt.afc_ops_service.repository.mongo;

import com.vdt.afc_ops_service.document.ControlPackagePayload;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ControlPackagePayloadRepository extends MongoRepository<ControlPackagePayload, String> {

    Optional<ControlPackagePayload> findByControlPackageId(Long controlPackageId);
}
