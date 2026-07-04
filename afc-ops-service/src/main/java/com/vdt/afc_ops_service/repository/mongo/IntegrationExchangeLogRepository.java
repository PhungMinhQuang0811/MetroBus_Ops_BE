package com.vdt.afc_ops_service.repository.mongo;

import com.vdt.afc_ops_service.document.IntegrationExchangeLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface IntegrationExchangeLogRepository extends MongoRepository<IntegrationExchangeLog, String> {
    Page<IntegrationExchangeLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<IntegrationExchangeLog> findBySystemNameAndTimestampBetweenOrderByTimestampDesc(String systemName, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<IntegrationExchangeLog> findByDirectionAndTimestampBetweenOrderByTimestampDesc(String direction, LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<IntegrationExchangeLog> findByStatusAndTimestampBetweenOrderByTimestampDesc(String status, LocalDateTime start, LocalDateTime end, Pageable pageable);
}
